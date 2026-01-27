package com.gyeongsan.cabinet.admin.dto;

import com.gyeongsan.cabinet.user.domain.User;

import java.time.LocalDateTime;

public record PenaltyUserResponse(
        Long userId,
        String name,
        Integer penaltyDays,
        LocalDateTime penaltyEndDate) {
    public static PenaltyUserResponse from(User user) {
        LocalDateTime penaltyEndDate = LocalDateTime.now().plusDays(user.getPenaltyDays());

        return new PenaltyUserResponse(
                user.getId(),
                user.getName(),
                user.getPenaltyDays(),
                penaltyEndDate);
    }
}
