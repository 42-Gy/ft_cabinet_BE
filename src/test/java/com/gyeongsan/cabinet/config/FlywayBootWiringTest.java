package com.gyeongsan.cabinet.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.gyeongsan.cabinet.adapter.out.persistence.admin.AdminActionLogEntity;
import com.gyeongsan.cabinet.support.MariaDbDriverMySqlContainer;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 실제 운영 설정(src/main/resources/application.yml 의 JPA/SQL 초기화 설정)으로 Spring Boot 가 Flyway 와 JPA 를 연결해
 * 부팅하는지 확인한다. 라이브러리를 직접 호출하는 테스트로는 잡히지 않는, Boot 의 초기화 순서(depends-on) 문제를 잡기 위한 테스트다. Docker 가 없으면
 * 건너뛴다.
 */
@Testcontainers(disabledWithoutDocker = true)
class FlywayBootWiringTest {

    private static final Path MAIN_YML = Path.of("src/main/resources/application.yml");

    /** 마이그레이션 대상인 admin_action_log 엔티티만 스캔한다. 다른 테이블은 이 DB 에 없다. */
    @Configuration
    @EntityScan(basePackageClasses = AdminActionLogEntity.class)
    static class EntityScanConfig {}

    private static ApplicationContextRunner runner(MariaDbDriverMySqlContainer mysql) {
        return new ApplicationContextRunner()
                .withInitializer(
                        context -> {
                            try {
                                // 운영과 같은 application.yml 을 그대로 읽는다 (가장 낮은 우선순위로 추가).
                                for (PropertySource<?> source :
                                        new YamlPropertySourceLoader()
                                                .load(
                                                        "main-application-yml",
                                                        new FileSystemResource(MAIN_YML))) {
                                    context.getEnvironment().getPropertySources().addLast(source);
                                }
                            } catch (java.io.IOException e) {
                                throw new IllegalStateException(e);
                            }
                        })
                .withConfiguration(
                        AutoConfigurations.of(
                                DataSourceAutoConfiguration.class,
                                DataSourceTransactionManagerAutoConfiguration.class,
                                JdbcTemplateAutoConfiguration.class,
                                SqlInitializationAutoConfiguration.class,
                                FlywayAutoConfiguration.class,
                                HibernateJpaAutoConfiguration.class))
                .withUserConfiguration(EntityScanConfig.class)
                .withPropertyValues(
                        // 운영에서 환경변수로 주입되는 값만 컨테이너 값으로 대체한다.
                        "spring.datasource.url=" + mysql.getJdbcUrl(),
                        "spring.datasource.username=" + mysql.getUsername(),
                        "spring.datasource.password=" + mysql.getPassword(),
                        "FLYWAY_ENABLED=true");
    }

    /** 기존 DB 처럼 비어 있지 않은 스키마에 사람이 baseline(v1)을 찍어 둔 상태를 만든다. */
    private static void prepareBaselinedExistingSchema(MariaDbDriverMySqlContainer mysql)
            throws Exception {
        try (Connection c =
                        DriverManager.getConnection(
                                mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
                Statement s = c.createStatement()) {
            s.execute("CREATE TABLE cabinet (id BIGINT PRIMARY KEY)");
        }
        Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .baselineVersion("1")
                .baselineOnMigrate(false)
                .load()
                .baseline();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.0", "mysql:8.4"})
    @DisplayName("FLYWAY_ENABLED=true 와 운영 yml 설정으로 부팅하면 Flyway 가 V2 를 적용한 뒤 JPA(validate)가 뜬다")
    void bootsWithFlywayEnabled_usingRealApplicationYml(String image) throws Exception {
        try (MariaDbDriverMySqlContainer mysql = new MariaDbDriverMySqlContainer(image)) {
            mysql.start();
            prepareBaselinedExistingSchema(mysql);

            runner(mysql)
                    .run(
                            context -> {
                                assertThat(context).hasNotFailed();
                                // 운영과 같은 ddl-auto=validate 로 떴다는 것은 Flyway 가 먼저 테이블을 만들었다는 뜻이다.
                                assertThat(
                                                context.getEnvironment()
                                                        .getProperty(
                                                                "spring.jpa.hibernate.ddl-auto"))
                                        .isEqualTo("validate");

                                DataSource dataSource = context.getBean(DataSource.class);
                                try (Connection c = dataSource.getConnection();
                                        Statement s = c.createStatement();
                                        ResultSet rs =
                                                s.executeQuery(
                                                        "SELECT version, success FROM flyway_schema_history"
                                                                + " ORDER BY installed_rank")) {
                                    List<String> applied = new java.util.ArrayList<>();
                                    while (rs.next()) {
                                        applied.add(
                                                rs.getString("version")
                                                        + ":"
                                                        + rs.getInt("success"));
                                    }
                                    assertThat(applied).containsExactly("1:1", "2:1");
                                }
                            });
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.4"})
    @DisplayName(
            "기준선: defer-datasource-initialization=true 를 켜면 Flyway 와 순환 의존으로 부팅이 실패한다(이 테스트가 문제를 실제로 잡는다는 증거)")
    void deferDatasourceInitializationBreaksFlyway(String image) throws Exception {
        try (MariaDbDriverMySqlContainer mysql = new MariaDbDriverMySqlContainer(image)) {
            mysql.start();
            prepareBaselinedExistingSchema(mysql);

            runner(mysql)
                    .withPropertyValues("spring.jpa.defer-datasource-initialization=true")
                    .run(
                            context -> {
                                assertThat(context).hasFailed();
                                assertThat(context.getStartupFailure())
                                        .hasStackTraceContaining(
                                                "Circular depends-on relationship");
                            });
        }
    }
}
