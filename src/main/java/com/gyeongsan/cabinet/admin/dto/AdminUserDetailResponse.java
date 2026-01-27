package com.gyeongsan.cabinet.admin.dto;

import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.domain.UserRole;
import java.time.LocalDateTime;

import java.util.Map;

public record AdminUserDetailResponse(
                Long id,
                String name,
                String email,
                Long coin,
                Integer penaltyDays,
                LocalDateTime blackholedAt,
                LocalDateTime unbannedAt,
                UserRole role,
                Integer currentCabinetNum,
                Integer monthlyLogtime,
                Map<String, Integer> itemCounts) {
        public static AdminUserDetailResponse of(User user, Integer currentCabinetNum,
                        Map<String, Integer> itemCounts) {
                LocalDateTime unbannedAtVal = (user.getPenaltyDays() != null && user.getPenaltyDays() > 0)
                                ? LocalDateTime.now().plusDays(user.getPenaltyDays())
                                : null;

                return new AdminUserDetailResponse(
                                user.getId(),
                                user.getName(),
                                user.getEmail(),
                                user.getCoin(),
                                user.getPenaltyDays(),
                                user.getBlackholedAt(),
                                unbannedAtVal,
                                user.getRole(),
                                currentCabinetNum,
                                user.getMonthlyLogtime(),
                                itemCounts);
        }
}