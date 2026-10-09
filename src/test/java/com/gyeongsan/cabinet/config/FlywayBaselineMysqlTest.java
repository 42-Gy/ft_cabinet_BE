package com.gyeongsan.cabinet.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gyeongsan.cabinet.support.BaselinedSchema;
import com.gyeongsan.cabinet.support.MariaDbDriverMySqlContainer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 운영 베이스라인(V1)과 V2~V6 가 실제 MySQL 8.0/8.4 에서 어떻게 맞물리는지, 그리고 그 스키마가 모든 엔티티와 {@code ddl-auto=validate}
 * 로 맞는지 확인한다. 테스트 서버를 update 에서 validate 로 바꿔도 되는지의 근거가 되는 테스트다. Docker 가 없으면 건너뛴다.
 */
@Testcontainers(disabledWithoutDocker = true)
class FlywayBaselineMysqlTest {

    /** 모든 엔티티를 스캔한다(운영 부팅과 같은 범위). */
    @Configuration
    @EntityScan("com.gyeongsan.cabinet")
    static class AllEntitiesConfig {}

    private static Flyway flyway(MariaDbDriverMySqlContainer mysql) {
        return Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .locations("classpath:db/migration")
                .baselineVersion("1")
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .validateOnMigrate(true)
                .load();
    }

    private static Connection connect(MariaDbDriverMySqlContainer mysql) throws Exception {
        return DriverManager.getConnection(
                mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
    }

    private static List<String> history(MariaDbDriverMySqlContainer mysql) throws Exception {
        List<String> rows = new ArrayList<>();
        try (Connection c = connect(mysql);
                Statement s = c.createStatement();
                ResultSet rs =
                        s.executeQuery(
                                "SELECT version, type, success FROM flyway_schema_history"
                                        + " ORDER BY installed_rank")) {
            while (rs.next()) {
                rows.add(rs.getString(1) + ":" + rs.getString(2) + ":" + rs.getInt(3));
            }
        }
        return rows;
    }

    /** 운영과 같은 ddl-auto=validate 로 모든 엔티티를 이 DB 에 대해 검증한다(Flyway 는 끈다). */
    private static void assertEntitiesValidate(MariaDbDriverMySqlContainer mysql) {
        new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(
                                DataSourceAutoConfiguration.class,
                                DataSourceTransactionManagerAutoConfiguration.class,
                                HibernateJpaAutoConfiguration.class))
                .withUserConfiguration(AllEntitiesConfig.class)
                .withPropertyValues(
                        "spring.datasource.url=" + mysql.getJdbcUrl(),
                        "spring.datasource.username=" + mysql.getUsername(),
                        "spring.datasource.password=" + mysql.getPassword(),
                        "spring.jpa.hibernate.ddl-auto=validate",
                        "spring.jpa.open-in-view=false")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.0", "mysql:8.4", "mariadb:10.6"})
    @DisplayName("새 DB(로컬 compose 의 mariadb:10.6 포함): V1~V6 가 순서대로 적용되고 모든 엔티티가 validate 를 통과한다")
    void freshDatabaseMigratesAndValidates(String image) throws Exception {
        try (MariaDbDriverMySqlContainer mysql = new MariaDbDriverMySqlContainer(image)) {
            mysql.start();

            MigrateResult result = flyway(mysql).migrate();

            assertThat(result.migrationsExecuted).isEqualTo(6);
            assertThat(history(mysql))
                    .containsExactly(
                            "1:SQL:1", "2:SQL:1", "3:SQL:1", "4:SQL:1", "5:SQL:1", "6:SQL:1");
            assertEntitiesValidate(mysql);
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.0", "mysql:8.4"})
    @DisplayName(
            "운영 경로: 기존 스키마에 baseline(1)만 찍으면 V1 은 실행되지 않고(데이터 보존) V2~V6 만 적용되며, 엔티티가 validate 를 통과한다")
    void productionPathSkipsV1AndKeepsData(String image) throws Exception {
        try (MariaDbDriverMySqlContainer mysql = new MariaDbDriverMySqlContainer(image)) {
            mysql.start();
            // 운영처럼 이미 있는 스키마 + 데이터. (V1 을 JDBC 로 직접 실행 = 사람이 만들어 둔 상태)
            BaselinedSchema.createExistingTables(mysql);
            try (Connection c = connect(mysql);
                    Statement s = c.createStatement()) {
                s.execute("INSERT INTO `user` (name, is_pisciner) VALUES ('existing-user', b'0')");
            }

            Flyway flyway = flyway(mysql);
            flyway.baseline();
            MigrateResult result = flyway.migrate();

            assertThat(result.migrationsExecuted).isEqualTo(5);
            assertThat(history(mysql))
                    .containsExactly(
                            "1:BASELINE:1", "2:SQL:1", "3:SQL:1", "4:SQL:1", "5:SQL:1", "6:SQL:1");
            // V1 이 실행됐다면 (CREATE TABLE 충돌로 실패하거나, DROP 이 있었다면 데이터가 사라졌을 것이다.)
            try (Connection c = connect(mysql);
                    Statement s = c.createStatement();
                    ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM `user`")) {
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(1);
            }
            flyway.validate();
            assertEntitiesValidate(mysql);
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.4"})
    @DisplayName("운영 경로: 새 DB 경로와 운영 경로가 만든 최종 스키마(컬럼·인덱스·FK)가 같다")
    void bothPathsEndWithSameSchema(String image) throws Exception {
        List<String> fresh;
        List<String> production;
        try (MariaDbDriverMySqlContainer mysql = new MariaDbDriverMySqlContainer(image)) {
            mysql.start();
            flyway(mysql).migrate();
            fresh = describeSchema(mysql);
        }
        try (MariaDbDriverMySqlContainer mysql = new MariaDbDriverMySqlContainer(image)) {
            mysql.start();
            BaselinedSchema.prepare(mysql);
            flyway(mysql).migrate();
            production = describeSchema(mysql);
        }
        assertThat(production).isEqualTo(fresh);
        assertThat(fresh)
                .anyMatch(
                        r -> r.startsWith("IDX|cabinet|idx_cabinet_visible_num|1|visible_num|0|"));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.4"})
    @DisplayName(
            "baseline 을 찍지 않은 기존 DB 에서 migrate 하면 Flyway 가 거부한다(baselineOnMigrate=false 의 안전망)")
    void migrateWithoutBaselineIsRefused(String image) throws Exception {
        try (MariaDbDriverMySqlContainer mysql = new MariaDbDriverMySqlContainer(image)) {
            mysql.start();
            BaselinedSchema.createExistingTables(mysql);

            assertThatThrownBy(() -> flyway(mysql).migrate()).isInstanceOf(FlywayException.class);
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.4"})
    @DisplayName(
            "테스트 서버(ddl-auto=update 로 이미 V6 까지 반영된 스키마, 이력 없음): baseline(6)로 맞추면 적용할 것이 없고, baseline(1)은 V2 에서 안전하게 실패한다")
    void updateBuiltSchemaNeedsBaselineAtLatestVersion(String image) throws Exception {
        try (MariaDbDriverMySqlContainer mysql = new MariaDbDriverMySqlContainer(image)) {
            mysql.start();
            flyway(mysql).migrate(); // V1~V6 까지 반영된 완성 스키마
            try (Connection c = connect(mysql);
                    Statement s = c.createStatement()) {
                s.execute("DROP TABLE flyway_schema_history"); // update 로 만든 DB 처럼 이력이 없는 상태
            }

            // 잘못된 선택: baseline(1) 이면 V2 가 이미 있는 테이블을 만들려다 실패한다(데이터는 건드리지 않음).
            Flyway wrong = flyway(mysql);
            wrong.baseline();
            assertThatThrownBy(wrong::migrate).isInstanceOf(FlywayException.class);

            try (Connection c = connect(mysql);
                    Statement s = c.createStatement()) {
                s.execute("DROP TABLE flyway_schema_history");
            }

            Flyway right =
                    Flyway.configure()
                            .dataSource(
                                    mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                            .locations("classpath:db/migration")
                            .baselineVersion("6")
                            .baselineOnMigrate(false)
                            .cleanDisabled(true)
                            .load();
            right.baseline();
            assertThat(right.migrate().migrationsExecuted).isZero();
            assertEntitiesValidate(mysql);
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.4"})
    @DisplayName("V1 이 없던 시절 V2~V4 만 적용된 이력(V1 행 없음)은 V1 파일이 생기면 검증에서 막힌다 — 이력을 정리해야 한다(알려진 함정)")
    void historyWithoutV1RowFailsValidation(String image) throws Exception {
        try (MariaDbDriverMySqlContainer mysql = new MariaDbDriverMySqlContainer(image)) {
            mysql.start();
            BaselinedSchema.prepare(mysql);
            flyway(mysql).migrate();
            try (Connection c = connect(mysql);
                    Statement s = c.createStatement()) {
                s.execute("DELETE FROM flyway_schema_history WHERE version = '1'");
            }

            assertThatThrownBy(() -> flyway(mysql).migrate())
                    .isInstanceOf(FlywayException.class)
                    .hasMessageContaining("Detected resolved migration not applied to database: 1");
        }
    }

    private static List<String> describeSchema(MariaDbDriverMySqlContainer mysql) throws Exception {
        String[] queries = {
            "SELECT CONCAT_WS('|','COL',table_name,column_name,ordinal_position,column_type,is_nullable,"
                    + "IFNULL(column_default,'<null>'),extra) FROM information_schema.columns"
                    + " WHERE table_schema=DATABASE() AND table_name <> 'flyway_schema_history'",
            "SELECT CONCAT_WS('|','IDX',table_name,index_name,seq_in_index,column_name,non_unique,"
                    + "IFNULL(collation,'')) FROM information_schema.statistics"
                    + " WHERE table_schema=DATABASE() AND table_name <> 'flyway_schema_history'",
            "SELECT CONCAT_WS('|','FK',k.table_name,k.constraint_name,k.column_name,"
                    + "k.referenced_table_name,k.referenced_column_name) FROM"
                    + " information_schema.key_column_usage k WHERE k.table_schema=DATABASE()"
                    + " AND k.referenced_table_name IS NOT NULL"
        };
        List<String> rows = new ArrayList<>();
        try (Connection c = connect(mysql);
                Statement s = c.createStatement()) {
            for (String q : queries) {
                try (ResultSet rs = s.executeQuery(q)) {
                    while (rs.next()) {
                        rows.add(rs.getString(1));
                    }
                }
            }
        }
        java.util.Collections.sort(rows);
        return rows;
    }
}
