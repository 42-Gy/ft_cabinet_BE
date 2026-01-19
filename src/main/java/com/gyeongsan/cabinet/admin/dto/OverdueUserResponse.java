package com.gyeongsan.cabinet.admin.dto;

import com.gyeongsan.cabinet.lent.domain.LentHistory;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record OverdueUserResponse(
        Long userId,
        String name,
        Integer visibleNum,
        Long overdueDays) {
    public static OverdueUserResponse from(LentHistory lentHistory) {
        long overdueDays = ChronoUnit.DAYS.between(lentHistory.getExpiredAt(), LocalDateTime.now());
        if (overdueDays < 0)
            overdueDays = 0;

        return new OverdueUserResponse(
                lentHistory.getUser().getId(),
                lentHistory.getUser().getName(),
                lentHistory.getCabinet().getVisibleNum(),
                overdueDays);
    }
}
