package com.gyeongsan.cabinet.adapter.in.web.admin.dto;

import com.gyeongsan.cabinet.domain.admin.model.AdminActionLog;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionTargetType;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 관리자 작업 기록 상세. 대상별 변경 전/후 값을 포함한다. */
public record AdminActionLogDetailResponse(
        String batchId,
        AdminActionType actionType,
        Long actorId,
        String actorName,
        String reason,
        LocalDateTime createdAt,
        String undoOfBatchId,
        String undoneByBatchId,
        Map<String, Object> request,
        List<Item> items) {

    public record Item(
            AdminActionTargetType targetType,
            Long targetId,
            String targetLabel,
            Map<String, Object> before,
            Map<String, Object> after) {}

    public static AdminActionLogDetailResponse of(AdminActionLog log, String undoneByBatchId) {
        return new AdminActionLogDetailResponse(
                log.batchId(),
                log.actionType(),
                log.actor().id(),
                log.actor().name(),
                log.reason(),
                log.createdAt(),
                log.undoOfBatchId(),
                undoneByBatchId,
                log.request(),
                log.items().stream()
                        .map(
                                i ->
                                        new Item(
                                                i.targetType(),
                                                i.targetId(),
                                                i.targetLabel(),
                                                i.before(),
                                                i.after()))
                        .toList());
    }
}
