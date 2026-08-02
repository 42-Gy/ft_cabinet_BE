package com.gyeongsan.cabinet.adapter.in.web.admin.dto;

import com.gyeongsan.cabinet.domain.user.model.UserRole;
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
        Integer currentCabinetNum) {}
