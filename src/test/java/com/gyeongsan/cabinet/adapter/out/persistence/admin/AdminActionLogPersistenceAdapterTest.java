package com.gyeongsan.cabinet.adapter.out.persistence.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionLog;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionLogItem;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionTargetType;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionType;
import com.gyeongsan.cabinet.domain.admin.model.AdminActor;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminActionLogPersistenceAdapterTest {

    @Mock private AdminActionLogJpaRepository repository;

    @Test
    @DisplayName("도메인 로그를 엔티티로 옮기고 요청과 전/후 값을 JSON 으로 직렬화한다")
    void save_mapsDomainLogToEntityWithJson() {
        AdminActionLogPersistenceAdapter adapter =
                new AdminActionLogPersistenceAdapter(repository, new ObjectMapper());

        Map<String, Object> before = new LinkedHashMap<>();
        before.put("status", "FULL");
        before.put("statusNote", null);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("status", "AVAILABLE");
        after.put("statusNote", "정리 완료");

        LocalDateTime now = LocalDateTime.of(2026, 10, 5, 12, 0, 0);
        adapter.save(
                new AdminActionLog(
                        "batch-1",
                        AdminActionType.CABINET_BULK_STATUS_UPDATE,
                        new AdminActor(1L, "admin01"),
                        "월말 일괄 반납",
                        Map.of("cabinetIds", List.of(1L, 2L)),
                        now,
                        List.of(
                                new AdminActionLogItem(
                                        AdminActionTargetType.CABINET, 7L, "101", before, after),
                                new AdminActionLogItem(
                                        AdminActionTargetType.LENT_HISTORY,
                                        9L,
                                        "101",
                                        null,
                                        null))));

        ArgumentCaptor<AdminActionLogEntity> captor =
                ArgumentCaptor.forClass(AdminActionLogEntity.class);
        verify(repository).save(captor.capture());
        AdminActionLogEntity entity = captor.getValue();

        assertThat(entity.getBatchId()).isEqualTo("batch-1");
        assertThat(entity.getActionType()).isEqualTo(AdminActionType.CABINET_BULK_STATUS_UPDATE);
        assertThat(entity.getActorId()).isEqualTo(1L);
        assertThat(entity.getActorName()).isEqualTo("admin01");
        assertThat(entity.getReason()).isEqualTo("월말 일괄 반납");
        assertThat(entity.getCreatedAt()).isEqualTo(now);
        assertThat(entity.getRequestJson()).isEqualTo("{\"cabinetIds\":[1,2]}");

        assertThat(entity.getItems()).hasSize(2);
        AdminActionLogItemEntity cabinetItem = entity.getItems().get(0);
        assertThat(cabinetItem.getLog()).isSameAs(entity);
        assertThat(cabinetItem.getTargetType()).isEqualTo(AdminActionTargetType.CABINET);
        assertThat(cabinetItem.getTargetId()).isEqualTo(7L);
        assertThat(cabinetItem.getTargetLabel()).isEqualTo("101");
        assertThat(cabinetItem.getBeforeJson())
                .isEqualTo("{\"status\":\"FULL\",\"statusNote\":null}");
        assertThat(cabinetItem.getAfterJson())
                .isEqualTo("{\"status\":\"AVAILABLE\",\"statusNote\":\"정리 완료\"}");

        AdminActionLogItemEntity lentItem = entity.getItems().get(1);
        assertThat(lentItem.getBeforeJson()).isNull();
        assertThat(lentItem.getAfterJson()).isNull();
    }

    @Test
    @DisplayName("저장된 엔티티를 도메인 로그로 읽을 때 JSON 을 Map 으로, 항목은 ID 순으로 복원한다")
    void findByBatchId_mapsEntityBackToDomainWithParsedJson() {
        AdminActionLogPersistenceAdapter adapter =
                new AdminActionLogPersistenceAdapter(repository, new ObjectMapper());
        LocalDateTime now = LocalDateTime.of(2026, 10, 5, 12, 0, 0);
        AdminActionLogEntity entity =
                new AdminActionLogEntity(
                        "batch-1",
                        AdminActionType.CABINET_BULK_STATUS_UNDO,
                        1L,
                        "admin01",
                        "되돌림",
                        "{\"undoOfBatchId\":\"orig\"}",
                        now,
                        "orig");
        AdminActionLogItemEntity second =
                new AdminActionLogItemEntity(
                        entity,
                        AdminActionTargetType.LENT_HISTORY,
                        9L,
                        "101",
                        "{\"endedAt\":\"2026-10-05T12:00:00.123456\",\"userId\":7}",
                        "{\"endedAt\":null,\"userId\":7}");
        AdminActionLogItemEntity first =
                new AdminActionLogItemEntity(
                        entity,
                        AdminActionTargetType.CABINET,
                        7L,
                        "101",
                        "{\"status\":\"AVAILABLE\",\"statusNote\":null}",
                        "{\"status\":\"FULL\",\"statusNote\":null}");
        org.springframework.test.util.ReflectionTestUtils.setField(first, "id", 1L);
        org.springframework.test.util.ReflectionTestUtils.setField(second, "id", 2L);
        // 일부러 뒤섞어 넣어도 ID 순으로 복원되어야 한다.
        entity.addItem(second);
        entity.addItem(first);
        org.mockito.BDDMockito.given(repository.findByBatchId("batch-1"))
                .willReturn(java.util.Optional.of(entity));

        AdminActionLog log = adapter.findByBatchId("batch-1").orElseThrow();

        assertThat(log.actionType()).isEqualTo(AdminActionType.CABINET_BULK_STATUS_UNDO);
        assertThat(log.undoOfBatchId()).isEqualTo("orig");
        assertThat(log.actor()).isEqualTo(new AdminActor(1L, "admin01"));
        assertThat(log.request()).containsEntry("undoOfBatchId", "orig");
        assertThat(log.items()).hasSize(2);
        assertThat(log.items().get(0).targetType()).isEqualTo(AdminActionTargetType.CABINET);
        assertThat(log.items().get(0).before())
                .containsEntry("status", "AVAILABLE")
                .containsEntry("statusNote", null);
        assertThat(log.items().get(1).before())
                .containsEntry("endedAt", "2026-10-05T12:00:00.123456");
        assertThat(((Number) log.items().get(1).before().get("userId")).longValue()).isEqualTo(7L);
        assertThat(log.items().get(1).after()).containsEntry("endedAt", null);
    }

    @Test
    @DisplayName("원본이 없으면 비어 있다")
    void findByBatchId_missing_isEmpty() {
        AdminActionLogPersistenceAdapter adapter =
                new AdminActionLogPersistenceAdapter(repository, new ObjectMapper());
        org.mockito.BDDMockito.given(repository.findByBatchId("none"))
                .willReturn(java.util.Optional.empty());

        assertThat(adapter.findByBatchId("none")).isEmpty();
    }
}
