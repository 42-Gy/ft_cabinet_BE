package com.gyeongsan.cabinet.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.testcontainers.containers.MySQLContainer;

/**
 * 운영 DB 와 같은 출발점을 흉내 낸다: 이미 테이블이 있는 DB 에 사람이 baseline(v1)을 한 번 찍은 상태.
 *
 * <p>마이그레이션은 기존 테이블을 건드린다(예: V4 는 {@code user} 에 컬럼 추가). 그래서 실제 베이스라인 덤프 대신 그 마이그레이션이 참조하는 최소한의
 * 테이블만 만든다. baselineOnMigrate 는 켜지 않는다(규칙 4).
 */
public final class BaselinedSchema {

    private BaselinedSchema() {}

    public static void createExistingTables(MySQLContainer<?> mysql) throws SQLException {
        try (Connection c =
                        DriverManager.getConnection(
                                mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
                Statement s = c.createStatement()) {
            s.execute("CREATE TABLE cabinet (id BIGINT PRIMARY KEY)");
            s.execute("CREATE TABLE `user` (id BIGINT PRIMARY KEY, name VARCHAR(32) NOT NULL)");
        }
    }

    /** 기존 테이블을 만들고 baseline(v1)까지 찍는다. 이후 migrate 하면 V2 부터 적용된다. */
    public static void prepare(MySQLContainer<?> mysql) throws SQLException {
        createExistingTables(mysql);
        Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .baselineVersion("1")
                .baselineOnMigrate(false)
                .load()
                .baseline();
    }
}
