package com.gyeongsan.cabinet.adapter.out.persistence.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gyeongsan.cabinet.domain.admin.model.AdminActionTargetType;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionType;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.output.MigrateResult;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * 실제 운영/테스트 DB 와 같은 종류(MySQL 8.0/8.4)에서, 운영과 같은 방식(MariaDB 드라이버 + MariaDBDialect + Spring Boot 기본
 * 네이밍 전략)으로 마이그레이션 SQL 과 엔티티 매핑이 어긋나지 않는지 검증한다. Docker 가 없으면 건너뛴다.
 */
@Testcontainers(disabledWithoutDocker = true)
class AdminActionLogMigrationTest {

    /** 운영과 같이 MariaDB 드라이버/URL 로 MySQL 서버에 붙는 컨테이너. */
    static class MariaDbDriverMySqlContainer extends MySQLContainer<MariaDbDriverMySqlContainer> {

        MariaDbDriverMySqlContainer(String image) {
            super(DockerImageName.parse(image));
        }

        @Override
        public String getDriverClassName() {
            return "org.mariadb.jdbc.Driver";
        }

        @Override
        public String getJdbcUrl() {
            return "jdbc:mariadb://"
                    + getHost()
                    + ":"
                    + getMappedPort(MYSQL_PORT)
                    + "/"
                    + getDatabaseName();
        }
    }

    private static MySQLContainer<?> newMysql(String image) {
        return new MariaDbDriverMySqlContainer(image);
    }

    private static String mariaDbUrl(MySQLContainer<?> mysql) {
        return mysql.getJdbcUrl();
    }

    private static Flyway flyway(MySQLContainer<?> mysql) {
        return Flyway.configure()
                .dataSource(mariaDbUrl(mysql), mysql.getUsername(), mysql.getPassword())
                .locations("classpath:db/migration")
                .baselineVersion("1")
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .load();
    }

    private static SessionFactory validatingSessionFactory(MySQLContainer<?> mysql) {
        StandardServiceRegistry registry =
                new StandardServiceRegistryBuilder()
                        .applySetting(
                                "hibernate.connection.driver_class", "org.mariadb.jdbc.Driver")
                        .applySetting("hibernate.connection.url", mariaDbUrl(mysql))
                        .applySetting("hibernate.connection.username", mysql.getUsername())
                        .applySetting("hibernate.connection.password", mysql.getPassword())
                        .applySetting("hibernate.dialect", "org.hibernate.dialect.MariaDBDialect")
                        .applySetting(
                                "hibernate.physical_naming_strategy",
                                "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy")
                        // 운영과 같은 validate: 엔티티가 기대하는 테이블/컬럼/타입이 실제 DB 에 없으면 실패한다.
                        .applySetting("hibernate.hbm2ddl.auto", "validate")
                        .build();
        return new MetadataSources(registry)
                .addAnnotatedClass(AdminActionLogEntity.class)
                .addAnnotatedClass(AdminActionLogItemEntity.class)
                .buildMetadata()
                .buildSessionFactory();
    }

    private static void executeSql(MySQLContainer<?> mysql, String sql) throws SQLException {
        try (Connection c =
                        DriverManager.getConnection(
                                mariaDbUrl(mysql), mysql.getUsername(), mysql.getPassword());
                Statement s = c.createStatement()) {
            s.execute(sql);
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.0", "mysql:8.4"})
    @DisplayName("기존 스키마에 baseline(V1) 후 V2 가 적용되고, 엔티티가 validate 를 통과하며 저장/조회된다")
    void baselineThenMigrate_entitiesValidateAndPersist(String image) throws Exception {
        try (MySQLContainer<?> mysql = newMysql(image)) {
            mysql.start();
            // 기존 운영 DB 처럼 비어 있지 않은 스키마를 흉내 낸다.
            executeSql(mysql, "CREATE TABLE cabinet (id BIGINT PRIMARY KEY)");

            // 규칙 4: baseline 없이 비어 있지 않은 스키마에 migrate 하면 조용히 실행되지 않고 실패해야 한다.
            assertThatThrownBy(() -> flyway(mysql).migrate()).isInstanceOf(FlywayException.class);

            // 사람이 1회 수동으로 찍는 baseline 과 같은 효과.
            flyway(mysql).baseline();
            MigrateResult result = flyway(mysql).migrate();
            assertThat(result.migrationsExecuted).isEqualTo(1);
            assertThat(flyway(mysql).info().current().getVersion().getVersion()).isEqualTo("2");
            // 두 번째 실행은 아무것도 하지 않는다.
            assertThat(flyway(mysql).migrate().migrationsExecuted).isZero();

            try (SessionFactory sessionFactory = validatingSessionFactory(mysql)) {
                persistSampleLog(sessionFactory);
            }

            try (Connection c =
                            DriverManager.getConnection(
                                    mariaDbUrl(mysql), mysql.getUsername(), mysql.getPassword());
                    Statement s = c.createStatement()) {
                try (ResultSet rs =
                        s.executeQuery(
                                "SELECT l.batch_id, l.actor_name, l.reason, l.request_json,"
                                        + " i.target_type, i.target_id, i.before_json"
                                        + " FROM admin_action_log l"
                                        + " JOIN admin_action_log_item i ON i.log_id = l.id")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("batch_id")).isEqualTo("batch-0001");
                    assertThat(rs.getString("actor_name")).isEqualTo("admin01");
                    assertThat(rs.getString("reason")).isEqualTo("월말 일괄 반납");
                    assertThat(rs.getString("request_json")).contains("\"status\"");
                    assertThat(rs.getString("target_type")).isEqualTo("CABINET");
                    assertThat(rs.getLong("target_id")).isEqualTo(7L);
                    assertThat(rs.getString("before_json")).contains("FULL");
                }
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.4"})
    @DisplayName("같은 batchId 로 두 번 저장할 수 없다")
    void batchIdIsUnique(String image) throws Exception {
        try (MySQLContainer<?> mysql = newMysql(image)) {
            mysql.start();
            flyway(mysql).migrate();

            try (SessionFactory sessionFactory = validatingSessionFactory(mysql)) {
                persistSampleLog(sessionFactory);
                assertThatThrownBy(() -> persistSampleLog(sessionFactory))
                        .isInstanceOf(Exception.class);
            }
        }
    }

    private static void persistSampleLog(SessionFactory sessionFactory) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            AdminActionLogEntity log =
                    new AdminActionLogEntity(
                            "batch-0001",
                            AdminActionType.CABINET_BULK_STATUS_UPDATE,
                            1L,
                            "admin01",
                            "월말 일괄 반납",
                            "{\"status\":\"AVAILABLE\"}",
                            LocalDateTime.now());
            log.addItem(
                    new AdminActionLogItemEntity(
                            log,
                            AdminActionTargetType.CABINET,
                            7L,
                            "101",
                            "{\"status\":\"FULL\"}",
                            "{\"status\":\"AVAILABLE\"}"));
            session.persist(log);
            session.getTransaction().commit();
        }
    }
}
