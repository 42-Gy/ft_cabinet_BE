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
}
