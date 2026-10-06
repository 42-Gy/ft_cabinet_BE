package com.gyeongsan.cabinet.adapter.out.persistence.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionLog;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionLogItem;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionLogSummary;
import com.gyeongsan.cabinet.domain.admin.model.AdminActor;
import com.gyeongsan.cabinet.domain.admin.port.out.AdminActionLogPort;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminActionLogPersistenceAdapter implements AdminActionLogPort {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE =
            new TypeReference<>() {};

    private final AdminActionLogJpaRepository adminActionLogJpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void save(AdminActionLog log) {
        AdminActionLogEntity entity =
                new AdminActionLogEntity(
                        log.batchId(),
                        log.actionType(),
                        log.actor().id(),
                        log.actor().name(),
                        log.reason(),
                        toJson(log.request()),
                        log.createdAt(),
                        log.undoOfBatchId());

        for (AdminActionLogItem item : log.items()) {
            entity.addItem(
                    new AdminActionLogItemEntity(
                            entity,
                            item.targetType(),
                            item.targetId(),
                            item.targetLabel(),
                            item.before() == null ? null : toJson(item.before()),
                            item.after() == null ? null : toJson(item.after())));
        }

        adminActionLogJpaRepository.save(entity);
    }

    @Override
    public Optional<AdminActionLog> findByBatchId(String batchId) {
        return adminActionLogJpaRepository.findByBatchId(batchId).map(this::toDomain);
    }

    @Override
    public Optional<String> findUndoBatchIdOf(String originalBatchId) {
        return adminActionLogJpaRepository.findUndoBatchIdOf(originalBatchId);
    }

    @Override
    public Page<AdminActionLogSummary> findSummaries(Pageable pageable) {
        return adminActionLogJpaRepository
                .findSummaries(pageable)
                .map(
                        v ->
                                new AdminActionLogSummary(
                                        v.getBatchId(),
                                        v.getActionType(),
                                        v.getActorId(),
                                        v.getActorName(),
                                        v.getReason(),
                                        v.getCreatedAt(),
                                        v.getItemCount() == null ? 0 : v.getItemCount(),
                                        v.getUndoOfBatchId(),
                                        v.getUndoneByBatchId()));
    }

    private AdminActionLog toDomain(AdminActionLogEntity entity) {
        return new AdminActionLog(
                entity.getBatchId(),
                entity.getActionType(),
                new AdminActor(entity.getActorId(), entity.getActorName()),
                entity.getReason(),
                fromJson(entity.getRequestJson()),
                entity.getCreatedAt(),
                entity.getItems().stream()
                        .sorted(Comparator.comparing(AdminActionLogItemEntity::getId))
                        .map(
                                i ->
                                        new AdminActionLogItem(
                                                i.getTargetType(),
                                                i.getTargetId(),
                                                i.getTargetLabel(),
                                                fromJson(i.getBeforeJson()),
                                                fromJson(i.getAfterJson())))
                        .toList(),
                entity.getUndoOfBatchId());
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            // 감사 기록을 못 남기면 그 작업도 롤백되어야 하므로 삼키지 않고 던진다.
            throw new IllegalStateException("감사 로그 직렬화에 실패했습니다.", e);
        }
    }

    private Map<String, Object> fromJson(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("감사 로그 역직렬화에 실패했습니다.", e);
        }
    }
}
