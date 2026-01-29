package com.gyeongsan.cabinet.admin.dto;

import com.gyeongsan.cabinet.user.domain.UserRole;
import java.time.LocalDateTime;

public record AdminAllUsersResponseDto(
        Long id,
        String name,
        String email,
        UserRole role,
        Long coin,
        Integer penaltyDays,
        Integer monthlyLogtime,
        LocalDateTime blackholedAt,
        boolean isRenting,
        Integer currentCabinetNum) {
}
