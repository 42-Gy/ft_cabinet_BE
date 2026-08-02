package com.gyeongsan.cabinet.adapter.in.web.admin.dto;

import com.gyeongsan.cabinet.domain.lent.model.LentHistory;
import java.time.LocalDateTime;

public record CabinetHistoryResponse(
        Long lentHistoryId, String userName, LocalDateTime startedAt, LocalDateTime endedAt) {
    public static CabinetHistoryResponse from(LentHistory history) {
        return new CabinetHistoryResponse(
                history.getId(),
                history.getUser().getName(),
                history.getStartedAt(),
                history.getEndedAt());
    }
}
