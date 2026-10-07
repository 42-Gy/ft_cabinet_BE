package com.gyeongsan.cabinet.adapter.out.persistence.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gyeongsan.cabinet.domain.admin.model.AdminActionTargetType;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionType;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.support.BaselinedSchema;
import com.gyeongsan.cabinet.support.MariaDbDriverMySqlContainer;
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

/**
 * 실제 운영/테스트 DB 와 같은 종류(MySQL 8.0/8.4)에서, 운영과 같은 방식(MariaDB 드라이버 + MariaDBDialect + Spring Boot 기본
 * 네이밍 전략)으로 마이그레이션 SQL 과 엔티티 매핑이 어긋나지 않는지 검증한다. Docker 가 없으면 건너뛴다.
 */
@Testcontainers(disabledWithoutDocker = true)
class AdminActionLogMigrationTest {

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
            BaselinedSchema.createExistingTables(mysql);

            // 규칙 4: baseline 없이 비어 있지 않은 스키마에 migrate 하면 조용히 실행되지 않고 실패해야 한다.
            assertThatThrownBy(() -> flyway(mysql).migrate()).isInstanceOf(FlywayException.class);

            // 사람이 1회 수동으로 찍는 baseline 과 같은 효과.
            flyway(mysql).baseline();
            MigrateResult result = flyway(mysql).migrate();
            assertThat(result.migrationsExecuted).isEqualTo(5);
            assertThat(flyway(mysql).info().current().getVersion().getVersion()).isEqualTo("6");
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
    @ValueSource(strings = {"mysql:8.0", "mysql:8.4"})
    @DisplayName(
            "V4: 기존 user 테이블에 ft_grade 가 추가되고, 기존 행은 NULL 이며, User 엔티티가 validate 를 통과해 저장/조회된다")
    void v4AddsFtGradeToExistingUserTable(String image) throws Exception {
        try (MySQLContainer<?> mysql = newMysql(image)) {
            mysql.start();
            // ft_grade 가 없는 기존 운영 user 테이블과 이미 들어 있는 사용자 한 명.
            executeSql(
                    mysql,
                    "CREATE TABLE `user` ("
                            + " id BIGINT AUTO_INCREMENT PRIMARY KEY, version BIGINT,"
                            + " name VARCHAR(32) NOT NULL UNIQUE, email VARCHAR(255) UNIQUE,"
                            + " role VARCHAR(255) NOT NULL, coin BIGINT NOT NULL,"
                            + " penalty_days INT NOT NULL, monthly_logtime INT NOT NULL,"
                            + " blackholed_at DATETIME(6), deleted_at DATETIME(6),"
                            + " slack_alarm BIT(1), email_alarm BIT(1), push_alarm BIT(1),"
                            + " is_pisciner BIT(1) NOT NULL)");
            executeSql(
                    mysql,
                    "INSERT INTO `user` (version, name, email, role, coin, penalty_days,"
                            + " monthly_logtime, is_pisciner)"
                            + " VALUES (0, 'existing-user', 'e@example.com', 'USER', 0, 0, 0, 0)");
            executeSql(mysql, "CREATE TABLE cabinet (id BIGINT PRIMARY KEY)");
            flyway(mysql).baseline();

            assertThat(flyway(mysql).migrate().migrationsExecuted).isEqualTo(5);

            try (Connection c =
                            DriverManager.getConnection(
                                    mariaDbUrl(mysql), mysql.getUsername(), mysql.getPassword());
                    Statement s = c.createStatement()) {
                try (ResultSet rs =
                        s.executeQuery(
                                "SELECT column_type, is_nullable FROM information_schema.columns"
                                        + " WHERE table_schema = DATABASE() AND table_name = 'user'"
                                        + " AND column_name = 'ft_grade'")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("column_type")).isEqualTo("varchar(32)");
                    assertThat(rs.getString("is_nullable")).isEqualTo("YES");
                }
                try (ResultSet rs =
                        s.executeQuery(
                                "SELECT ft_grade FROM `user` WHERE name = 'existing-user'")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("ft_grade")).isNull();
                }
            }

            StandardServiceRegistry registry =
                    new StandardServiceRegistryBuilder()
                            .applySetting(
                                    "hibernate.connection.driver_class", "org.mariadb.jdbc.Driver")
                            .applySetting("hibernate.connection.url", mariaDbUrl(mysql))
                            .applySetting("hibernate.connection.username", mysql.getUsername())
                            .applySetting("hibernate.connection.password", mysql.getPassword())
                            .applySetting(
                                    "hibernate.dialect", "org.hibernate.dialect.MariaDBDialect")
                            .applySetting(
                                    "hibernate.physical_naming_strategy",
                                    "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy")
                            .applySetting("hibernate.hbm2ddl.auto", "validate")
                            .build();
            try (SessionFactory sessionFactory =
                    new MetadataSources(registry)
                            .addAnnotatedClass(User.class)
                            .buildMetadata()
                            .buildSessionFactory()) {
                User transcender =
                        User.of("new-transcender", "t@example.com", UserRole.USER, false);
                transcender.updateFtGrade("Transcender");
                try (Session session = sessionFactory.openSession()) {
                    session.beginTransaction();
                    session.persist(transcender);
                    session.getTransaction().commit();
                }
                try (Session session = sessionFactory.openSession()) {
                    User loaded = session.find(User.class, transcender.getId());
                    assertThat(loaded.getFtGrade()).isEqualTo("Transcender");
                    assertThat(loaded.isTranscender()).isTrue();
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
            BaselinedSchema.prepare(mysql);
            flyway(mysql).migrate();

            try (SessionFactory sessionFactory = validatingSessionFactory(mysql)) {
                persistSampleLog(sessionFactory);
                assertThatThrownBy(() -> persistSampleLog(sessionFactory))
                        .isInstanceOf(Exception.class);
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.4"})
    @DisplayName("한 작업은 한 번만 되돌릴 수 있다(undo_of_batch_id UNIQUE), 일반 기록은 NULL 이 여러 개여도 된다")
    void undoOfBatchIdIsUniquePerOriginal(String image) throws Exception {
        try (MySQLContainer<?> mysql = newMysql(image)) {
            mysql.start();
            BaselinedSchema.prepare(mysql);
            flyway(mysql).migrate();

            try (SessionFactory sessionFactory = validatingSessionFactory(mysql)) {
                persistLog(
                        sessionFactory, "orig-1", AdminActionType.CABINET_BULK_STATUS_UPDATE, null);
                persistLog(
                        sessionFactory, "orig-2", AdminActionType.CABINET_BULK_STATUS_UPDATE, null);
                persistLog(
                        sessionFactory,
                        "undo-1",
                        AdminActionType.CABINET_BULK_STATUS_UNDO,
                        "orig-1");

                assertThatThrownBy(
                                () ->
                                        persistLog(
                                                sessionFactory,
                                                "undo-1b",
                                                AdminActionType.CABINET_BULK_STATUS_UNDO,
                                                "orig-1"))
                        .isInstanceOf(Exception.class);
                // 다른 원본에 대한 Undo 는 가능하다.
                persistLog(
                        sessionFactory,
                        "undo-2",
                        AdminActionType.CABINET_BULK_STATUS_UNDO,
                        "orig-2");
            }
        }
    }

    private static void persistSampleLog(SessionFactory sessionFactory) {
        persistLog(sessionFactory, "batch-0001", AdminActionType.CABINET_BULK_STATUS_UPDATE, null);
    }

    private static void persistLog(
            SessionFactory sessionFactory,
            String batchId,
            AdminActionType type,
            String undoOfBatchId) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            AdminActionLogEntity log =
                    new AdminActionLogEntity(
                            batchId,
                            type,
                            1L,
                            "admin01",
                            "월말 일괄 반납",
                            "{\"status\":\"AVAILABLE\"}",
                            LocalDateTime.now(),
                            undoOfBatchId);
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
