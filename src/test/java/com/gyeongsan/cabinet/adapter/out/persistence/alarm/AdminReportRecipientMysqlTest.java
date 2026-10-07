package com.gyeongsan.cabinet.adapter.out.persistence.alarm;

import static org.assertj.core.api.Assertions.assertThat;

import com.gyeongsan.cabinet.adapter.out.persistence.user.UserPersistenceAdapter;
import com.gyeongsan.cabinet.adapter.out.persistence.user.UserRepository;
import com.gyeongsan.cabinet.domain.alarm.port.out.ReportRecipientPort;
import com.gyeongsan.cabinet.support.MariaDbDriverMySqlContainer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 실제 MySQL 8.0/8.4 에서 수신자 쿼리를 확인한다: ADMIN·MASTER 만, 탈퇴한 유저와 일반 유저·이용 정지 유저는 제외. 스키마는 운영과 같은
 * Flyway(V1~V5)로 만든다.
 */
@Testcontainers(disabledWithoutDocker = true)
class AdminReportRecipientMysqlTest {

    @Configuration
    @EntityScan("com.gyeongsan.cabinet")
    @EnableJpaRepositories(basePackageClasses = UserRepository.class)
    @Import({UserPersistenceAdapter.class, AdminReportRecipientAdapter.class})
    static class TestConfig {}

    private static void insertUser(Connection c, String name, String role, boolean deleted)
            throws Exception {
        try (Statement s = c.createStatement()) {
            s.execute(
                    "INSERT INTO `user` (name, email, role, is_pisciner, deleted_at) VALUES ('"
                            + name
                            + "', '"
                            + name
                            + "@42gyeongsan.kr', '"
                            + role
                            + "', b'0', "
                            + (deleted ? "NOW(6)" : "NULL")
                            + ")");
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.0", "mysql:8.4"})
    @DisplayName("ADMIN·MASTER 권한의 탈퇴하지 않은 유저만 수신자가 된다(일반 유저·탈퇴자 제외)")
    void onlyActiveAdminsAndMasters(String image) throws Exception {
        try (MariaDbDriverMySqlContainer mysql = new MariaDbDriverMySqlContainer(image)) {
            mysql.start();
            Flyway.configure()
                    .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();
            try (Connection c =
                    DriverManager.getConnection(
                            mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())) {
                insertUser(c, "plain-user", "USER", false);
                insertUser(c, "admin-b", "ADMIN", false);
                insertUser(c, "master-a", "MASTER", false);
                insertUser(c, "admin-left", "ADMIN", true);
            }

            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    DataSourceAutoConfiguration.class,
                                    DataSourceTransactionManagerAutoConfiguration.class,
                                    HibernateJpaAutoConfiguration.class,
                                    JpaRepositoriesAutoConfiguration.class))
                    .withUserConfiguration(TestConfig.class)
                    .withPropertyValues(
                            "spring.datasource.url=" + mysql.getJdbcUrl(),
                            "spring.datasource.username=" + mysql.getUsername(),
                            "spring.datasource.password=" + mysql.getPassword(),
                            "spring.jpa.hibernate.ddl-auto=validate",
                            "spring.jpa.open-in-view=false")
                    .run(
                            context -> {
                                assertThat(context).hasNotFailed();
                                assertThat(
                                                context.getBean(ReportRecipientPort.class)
                                                        .findRecipientIntraIds())
                                        .containsExactly("admin-b", "master-a");
                            });
        }
    }
}
