package com.gyeongsan.cabinet.admin.dto;

import com.gyeongsan.cabinet.lent.domain.LentHistory;
import com.gyeongsan.cabinet.user.domain.User;
import java.time.LocalDateTime;

public record CabinetHistoryResponse(
        Long lentHistoryId,
        String userName,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {
    public static CabinetHistoryResponse from(LentHistory history) {
        return new CabinetHistoryResponse(
                history.getId(),
                history.getUser().getName(),
                history.getStartedAt(),
                history.getEndedAt()
        );
    }
}