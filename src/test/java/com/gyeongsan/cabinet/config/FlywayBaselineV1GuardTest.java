package com.gyeongsan.cabinet.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * V1(운영 스키마 베이스라인)에 위험한 구문이 섞이지 않았는지 지킨다. Docker 없이 파일만 읽는다.
 *
 * <p>V1 은 새 DB 를 만들 때만 실행되지만, 설정 실수로 운영에서 실행되는 경우를 가정하면 DROP/DELETE 같은 구문이 한 줄이라도 있는 순간 운영 데이터가
 * 사라진다. 그래서 CREATE TABLE 외에는 허용하지 않는다.
 */
class FlywayBaselineV1GuardTest {

    private static final Path V1 = Path.of("src/main/resources/db/migration/V1__baseline.sql");

    /** 이 12개가 운영 덤프에 있던 테이블이다(V2~V5 가 만드는 admin_*, faq* 는 제외). */
    private static final List<String> EXPECTED_TABLES =
            List.of(
                    "attendance",
                    "banned_user",
                    "cabinet",
                    "calendar_event",
                    "coin_history",
                    "item",
                    "item_history",
                    "lent_history",
                    "oauth_link",
                    "user",
                    "watermelon",
                    "watermelon_event_log");

    private static String sqlWithoutComments() throws Exception {
        String raw = Files.readString(V1, StandardCharsets.UTF_8);
        return raw.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)^\\s*--.*$", "");
    }

    @Test
    @DisplayName("V1 은 CREATE TABLE 만 담고, 데이터/삭제/권한/환경 의존 구문이 없다")
    void containsOnlyCreateTable() throws Exception {
        String sql = sqlWithoutComments();

        for (String statement : sql.split(";")) {
            String s = statement.strip();
            if (s.isEmpty()) {
                continue;
            }
            assertThat(s).as("모든 문장은 CREATE TABLE 이어야 한다: %s", s).startsWith("CREATE TABLE `");
        }
        assertThat(sql)
                .doesNotContainIgnoringCase("DROP ")
                .doesNotContainIgnoringCase("DELETE ")
                .doesNotContainIgnoringCase("TRUNCATE")
                .doesNotContainIgnoringCase("INSERT ")
                .doesNotContainIgnoringCase("UPDATE ")
                .doesNotContainIgnoringCase("DEFINER")
                .doesNotContainIgnoringCase("TRIGGER")
                .doesNotContainIgnoringCase("FOREIGN_KEY_CHECKS")
                .doesNotContainIgnoringCase("flyway_schema_history");
    }

    @Test
    @DisplayName("V1 에 덤프 시점 카운터(AUTO_INCREMENT=N)와 MySQL 8 전용 collation 이 남아 있지 않다")
    void hasNoEnvironmentSpecificTableOptions() throws Exception {
        String sql = sqlWithoutComments();

        assertThat(sql).doesNotContainPattern("AUTO_INCREMENT\\s*=");
        assertThat(sql).doesNotContainIgnoringCase("COLLATE");
        assertThat(sql).doesNotContainIgnoringCase("utf8mb4_0900");
    }

    @Test
    @DisplayName("V1 은 운영 덤프의 12개 테이블을 모두 담고, FK 부모 테이블이 자식보다 먼저 나온다")
    void hasExpectedTablesInDependencyOrder() throws Exception {
        String sql = sqlWithoutComments();

        Matcher created = Pattern.compile("CREATE TABLE `([^`]+)`").matcher(sql);
        List<String> order = new java.util.ArrayList<>();
        while (created.find()) {
            order.add(created.group(1));
        }
        assertThat(order).containsExactlyInAnyOrderElementsOf(EXPECTED_TABLES);

        // FK 가 참조하는 테이블은 참조하는 테이블보다 앞서 만들어져야 한다(FOREIGN_KEY_CHECKS 에 기대지 않는다).
        for (String statement : sql.split(";")) {
            Matcher self = Pattern.compile("CREATE TABLE `([^`]+)`").matcher(statement);
            if (!self.find()) {
                continue;
            }
            Matcher ref = Pattern.compile("REFERENCES `([^`]+)`").matcher(statement);
            while (ref.find()) {
                assertThat(order.indexOf(ref.group(1)))
                        .as("%s 가 참조하는 %s 는 먼저 만들어져야 한다", self.group(1), ref.group(1))
                        .isLessThan(order.indexOf(self.group(1)));
            }
        }
    }

    @Test
    @DisplayName("운영에 수동으로 걸었던 idx_cabinet_visible_num 유니크 인덱스가 베이스라인에 포함돼 있다")
    void capturesCabinetVisibleNumUniqueIndex() throws Exception {
        assertThat(sqlWithoutComments())
                .contains("UNIQUE KEY `idx_cabinet_visible_num` (`visible_num`)");
    }
}
