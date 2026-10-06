package com.gyeongsan.cabinet.domain.admin.model;

import java.time.LocalDateTime;

/** 감사 로그 목록용 요약. undoneByBatchId 가 있으면 이미 되돌려진 작업이다. */
public record AdminActionLogSummary(
        String batchId,
        AdminActionType actionType,
        Long actorId,
        String actorName,
        String reason,
        LocalDateTime createdAt,
        long itemCount,
        String undoOfBatchId,
        String undoneByBatchId) {}
