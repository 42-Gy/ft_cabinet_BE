package com.gyeongsan.cabinet.adapter.in.web.admin.dto;

import com.gyeongsan.cabinet.domain.admin.model.AdminActionLogSummary;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionType;
import java.time.LocalDateTime;

/** 관리자 작업 기록 목록의 한 줄. undoneByBatchId 가 있으면 이미 되돌려진 작업이다. */
public record AdminActionLogSummaryResponse(
        String batchId,
        AdminActionType actionType,
        Long actorId,
        String actorName,
        String reason,
        LocalDateTime createdAt,
        long itemCount,
        String undoOfBatchId,
        String undoneByBatchId) {

    public static AdminActionLogSummaryResponse from(AdminActionLogSummary summary) {
        return new AdminActionLogSummaryResponse(
                summary.batchId(),
                summary.actionType(),
                summary.actorId(),
                summary.actorName(),
                summary.reason(),
                summary.createdAt(),
                summary.itemCount(),
                summary.undoOfBatchId(),
                summary.undoneByBatchId());
    }
}
