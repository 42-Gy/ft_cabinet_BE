package com.gyeongsan.cabinet.adapter.out.persistence.cabinet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gyeongsan.cabinet.support.MariaDbDriverMySqlContainer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * {@code Cabinet} 엔티티의 {@code idx_cabinet_visible_num}(유니크) 선언이 두 환경에서 기대대로 동작하는지 실제 MySQL 로 확인한다.
 *
 * <ul>
 *   <li>테스트 서버처럼 인덱스 없이 만들어진 기존 테이블에 {@code ddl-auto=update} 로 부팅하면 인덱스가 자동으로 생긴다
 *   <li>같은 번호가 이미 중복돼 있으면 부팅은 실패하지 않고(경고만) 인덱스만 못 만든다 — 그래서 적용 전에 중복 확인이 필요하다
 * </ul>
 */
@Testcontainers(disabledWithoutDocker = true)
class CabinetVisibleNumIndexMysqlTest {

    /** 인덱스가 선언되기 전 Hibernate 가 만들던 cabinet 테이블(인덱스 없음). */
    private static final String OLD_CABINET_DDL =
            "create table cabinet (id bigint not null auto_increment, grid_col integer,"
                    + " floor integer, lent_type varchar(16) not null, max_user integer not null,"
                    + " grid_row integer, section varchar(255), status varchar(32) not null,"
                    + " status_note varchar(64), visible_num integer, primary key (id))";

    @Configuration
    @EntityScan(basePackages = "com.gyeongsan.cabinet")
    static class TestConfig {}

    private static void insertCabinet(Statement st, int visibleNum) throws Exception {
        st.execute(
                "insert into cabinet (floor, lent_type, max_user, status, visible_num)"
                        + " values (2, 'PRIVATE', 1, 'AVAILABLE', "
                        + visibleNum
                        + ")");
    }

    private static void withUpdateBoot(
            String image, boolean duplicates, Consumer<JdbcTemplate> body) throws Exception {
        try (MariaDbDriverMySqlContainer mysql = new MariaDbDriverMySqlContainer(image)) {
            mysql.start();
            try (Connection c =
                            DriverManager.getConnection(
                                    mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
                    Statement st = c.createStatement()) {
                st.execute(OLD_CABINET_DDL);
                insertCabinet(st, 1001);
                insertCabinet(st, duplicates ? 1001 : 1002);
            }
            Throwable[] failure = new Throwable[1];
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    DataSourceAutoConfiguration.class,
                                    JdbcTemplateAutoConfiguration.class,
                                    HibernateJpaAutoConfiguration.class))
                    .withUserConfiguration(TestConfig.class)
                    .withPropertyValues(
                            "spring.datasource.url=" + mysql.getJdbcUrl(),
                            "spring.datasource.username=" + mysql.getUsername(),
                            "spring.datasource.password=" + mysql.getPassword(),
                            "spring.datasource.driver-class-name=org.mariadb.jdbc.Driver",
                            "spring.jpa.hibernate.ddl-auto=update")
                    .run(
                            context -> {
                                assertThat(context).hasNotFailed();
                                try {
                                    body.accept(context.getBean(JdbcTemplate.class));
                                } catch (Throwable t) {
                                    failure[0] = t;
                                }
                            });
            if (failure[0] instanceof Error e) {
                throw e;
            } else if (failure[0] != null) {
                throw new RuntimeException(failure[0]);
            }
        }
    }

    private static List<Map<String, Object>> indexRows(JdbcTemplate jdbc) {
        return jdbc.queryForList(
                "select index_name, non_unique, column_name from information_schema.statistics"
                        + " where table_schema = database() and table_name = 'cabinet'"
                        + " and index_name = 'idx_cabinet_visible_num'");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.0", "mysql:8.4"})
    @DisplayName("인덱스 없이 만들어진 기존 테이블에 ddl-auto=update 로 부팅하면 유니크 인덱스가 자동으로 생기고, 중복 번호는 막힌다")
    void updateCreatesTheIndexOnExistingTable(String image) throws Exception {
        withUpdateBoot(
                image,
                false,
                jdbc -> {
                    List<Map<String, Object>> rows = indexRows(jdbc);
                    assertThat(rows).hasSize(1);
                    assertThat(((Number) rows.get(0).get("non_unique")).intValue()).isZero();
                    assertThat(String.valueOf(rows.get(0).get("column_name")))
                            .isEqualToIgnoringCase("visible_num");

                    assertThatThrownBy(
                                    () ->
                                            jdbc.update(
                                                    "insert into cabinet (floor, lent_type,"
                                                            + " max_user, status, visible_num) values"
                                                            + " (2, 'PRIVATE', 1, 'AVAILABLE', 1001)"))
                            .isInstanceOf(DataIntegrityViolationException.class);
                });
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.4"})
    @DisplayName("이미 같은 번호가 중복된 테이블이면 부팅은 되지만 인덱스는 못 만든다(그래서 적용 전에 중복 확인이 필요하다)")
    void duplicatesPreventIndexButNotBoot(String image) throws Exception {
        withUpdateBoot(image, true, jdbc -> assertThat(indexRows(jdbc)).isEmpty());
    }
}
