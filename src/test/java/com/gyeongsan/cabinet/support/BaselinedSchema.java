package com.gyeongsan.cabinet.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.flywaydb.core.Flyway;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.MySQLContainer;

/**
 * 운영 DB 와 같은 출발점을 흉내 낸다: 이미 테이블이 있는 DB 에 사람이 baseline(v1)을 한 번 찍은 상태.
 *
 * <p>운영 {@code mysqldump --no-data} 를 정제한 실제 베이스라인({@code V1__baseline.sql})을 JDBC 로 직접 실행해 기존 테이블을
 * 만든다. Flyway 가 아니라 직접 실행하는 이유: 운영에서도 V1 은 Flyway 가 실행하지 않고(사람이 baseline 만 찍음) 이미 존재하는 스키마이기 때문이다.
 * baselineOnMigrate 는 켜지 않는다(규칙 4).
 */
public final class BaselinedSchema {

    private BaselinedSchema() {}

    public static void createExistingTables(MySQLContainer<?> mysql) throws SQLException {
        try (Connection c =
                DriverManager.getConnection(
                        mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())) {
            ScriptUtils.executeSqlScript(c, new ClassPathResource("db/migration/V1__baseline.sql"));
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
