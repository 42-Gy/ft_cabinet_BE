package com.gyeongsan.cabinet.adapter.out.persistence.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionLog;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionLogItem;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionLogSummary;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionTargetType;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionType;
import com.gyeongsan.cabinet.domain.admin.model.AdminActor;
import com.gyeongsan.cabinet.support.BaselinedSchema;
import com.gyeongsan.cabinet.support.MariaDbDriverMySqlContainer;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 로그 조회 쿼리(서브쿼리 프로젝션, EntityGraph, Undo 유니크)를 실제 MySQL 에서 확인한다. 테이블은 운영과 같이 Flyway(V2, V3)로 만들고
 * Hibernate 는 validate 로 둔다. Docker 가 없으면 건너뛴다.
 */
@Testcontainers(disabledWithoutDocker = true)
class AdminActionLogRepositoryMysqlTest {

    @Configuration
    @EntityScan(basePackageClasses = AdminActionLogEntity.class)
    @EnableJpaRepositories(basePackageClasses = AdminActionLogJpaRepository.class)
    static class JpaConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        AdminActionLogPersistenceAdapter adapter(
                AdminActionLogJpaRepository repository, ObjectMapper objectMapper) {
            return new AdminActionLogPersistenceAdapter(repository, objectMapper);
        }
    }

    private static ApplicationContextRunner runner(MariaDbDriverMySqlContainer mysql) {
        return new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(
                                DataSourceAutoConfiguration.class,
                                DataSourceTransactionManagerAutoConfiguration.class,
                                TransactionAutoConfiguration.class,
                                FlywayAutoConfiguration.class,
                                HibernateJpaAutoConfiguration.class))
                .withUserConfiguration(JpaConfig.class)
                .withPropertyValues(
                        "spring.datasource.url=" + mysql.getJdbcUrl(),
                        "spring.datasource.username=" + mysql.getUsername(),
                        "spring.datasource.password=" + mysql.getPassword(),
                        "spring.datasource.driver-class-name=org.mariadb.jdbc.Driver",
                        "spring.flyway.enabled=true",
                        "spring.flyway.locations=classpath:db/migration",
                        "spring.jpa.hibernate.ddl-auto=validate",
                        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect");
    }

    private static AdminActionLog log(
            String batchId,
            AdminActionType type,
            LocalDateTime createdAt,
            String undoOf,
            List<AdminActionLogItem> items) {
        return new AdminActionLog(
                batchId,
                type,
                new AdminActor(1L, "admin01"),
                "월말 일괄 반납",
                Map.of("status", "AVAILABLE"),
                createdAt,
                items,
                undoOf);
    }

    private static AdminActionLogItem item(AdminActionTargetType type, long id, String status) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("status", "FULL");
        before.put("statusNote", null);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("status", status);
        after.put("statusNote", null);
        return new AdminActionLogItem(type, id, String.valueOf(100 + id), before, after);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.0", "mysql:8.4"})
    @DisplayName("목록은 최신순이고 항목 수와 Undo 여부를 한 번에 가져오며, 상세는 항목과 JSON 을 복원한다")
    void listDetailAndUndoLookup(String image) throws Exception {
        try (MariaDbDriverMySqlContainer mysql = new MariaDbDriverMySqlContainer(image)) {
            mysql.start();
            BaselinedSchema.prepare(mysql);
            runner(mysql)
                    .run(
                            context -> {
                                assertThat(context).hasNotFailed();
                                AdminActionLogPersistenceAdapter adapter =
                                        context.getBean(AdminActionLogPersistenceAdapter.class);

                                adapter.save(
                                        log(
                                                "orig-1",
                                                AdminActionType.CABINET_BULK_STATUS_UPDATE,
                                                LocalDateTime.of(2026, 10, 5, 12, 0),
                                                null,
                                                List.of(
                                                        item(
                                                                AdminActionTargetType.CABINET,
                                                                1,
                                                                "AVAILABLE"),
                                                        item(
                                                                AdminActionTargetType.CABINET,
                                                                2,
                                                                "AVAILABLE"))));
                                adapter.save(
                                        log(
                                                "orig-2",
                                                AdminActionType.CABINET_BULK_STATUS_UPDATE,
                                                LocalDateTime.of(2026, 10, 5, 13, 0),
                                                null,
                                                List.of()));
                                adapter.save(
                                        log(
                                                "undo-1",
                                                AdminActionType.CABINET_BULK_STATUS_UNDO,
                                                LocalDateTime.of(2026, 10, 5, 14, 0),
                                                "orig-1",
                                                List.of(
                                                        item(
                                                                AdminActionTargetType.CABINET,
                                                                1,
                                                                "FULL"))));

                                Page<AdminActionLogSummary> page =
                                        adapter.findSummaries(PageRequest.of(0, 10));
                                assertThat(page.getTotalElements()).isEqualTo(3);
                                assertThat(page.getContent())
                                        .extracting(AdminActionLogSummary::batchId)
                                        .containsExactly("undo-1", "orig-2", "orig-1");
                                AdminActionLogSummary orig1 = page.getContent().get(2);
                                assertThat(orig1.itemCount()).isEqualTo(2);
                                assertThat(orig1.undoneByBatchId()).isEqualTo("undo-1");
                                assertThat(page.getContent().get(1).undoneByBatchId()).isNull();
                                assertThat(page.getContent().get(0).undoOfBatchId())
                                        .isEqualTo("orig-1");

                                assertThat(adapter.findUndoBatchIdOf("orig-1")).contains("undo-1");
                                assertThat(adapter.findUndoBatchIdOf("orig-2")).isEmpty();

                                AdminActionLog detail =
                                        adapter.findByBatchId("orig-1").orElseThrow();
                                assertThat(detail.items()).hasSize(2);
                                assertThat(detail.items().get(0).targetId()).isEqualTo(1L);
                                assertThat(detail.items().get(0).before())
                                        .containsEntry("status", "FULL")
                                        .containsEntry("statusNote", null);
                                assertThat(detail.actor().name()).isEqualTo("admin01");
                                assertThat(adapter.findByBatchId("nope")).isEmpty();

                                // 같은 원본을 두 번 되돌릴 수 없다(DB UNIQUE).
                                assertThatThrownBy(
                                                () ->
                                                        adapter.save(
                                                                log(
                                                                        "undo-1b",
                                                                        AdminActionType
                                                                                .CABINET_BULK_STATUS_UNDO,
                                                                        LocalDateTime.of(
                                                                                2026, 10, 5, 15, 0),
                                                                        "orig-1",
                                                                        List.of())))
                                        .isInstanceOf(Exception.class);
                            });
        }
    }
}
