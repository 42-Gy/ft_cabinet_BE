package com.gyeongsan.cabinet.domain.admin.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 관리자 작업 1회의 감사 기록. 한 번 남기면 수정하거나 지우지 않는다(추가 전용).
 *
 * <p>undoOfBatchId 는 이 기록이 Undo 일 때, 되돌린 원래 작업의 batchId 이다.
 */
public record AdminActionLog(
        String batchId,
        AdminActionType actionType,
        AdminActor actor,
        String reason,
        Map<String, Object> request,
        LocalDateTime createdAt,
        List<AdminActionLogItem> items,
        String undoOfBatchId) {

    public static final int REASON_MAX_LENGTH = 255;

    /** 일반 작업 기록. Undo 기록이 아니므로 undoOfBatchId 는 없다. */
    public AdminActionLog(
            String batchId,
            AdminActionType actionType,
            AdminActor actor,
            String reason,
            Map<String, Object> request,
            LocalDateTime createdAt,
            List<AdminActionLogItem> items) {
        this(batchId, actionType, actor, reason, request, createdAt, items, null);
    }

    public AdminActionLog {
        if (batchId == null || batchId.isBlank() || actionType == null || actor == null) {
            throw new IllegalArgumentException("batchId, 작업 종류, 관리자는 필수입니다.");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("발생 시각은 필수입니다.");
        }
        if (reason != null && reason.length() > REASON_MAX_LENGTH) {
            throw new IllegalArgumentException("사유는 " + REASON_MAX_LENGTH + "자 이하여야 합니다.");
        }
        request =
                request == null
                        ? Map.of()
                        : Collections.unmodifiableMap(new LinkedHashMap<>(request));
        items = items == null ? List.of() : List.copyOf(items);
    }
}
