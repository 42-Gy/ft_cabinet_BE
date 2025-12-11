package com.gyeongsan.cabinet.admin.dto;

import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.domain.UserRole;
import java.time.LocalDateTime;

/**
 * 관리자 페이지에 보여줄 사용자 상세 정보 DTO
 */
public record AdminUserDetailResponse(
        Long id,
        String name,
        String email,
        Long coin,
        LocalDateTime blackholedAt,
        UserRole role
) {
    public static AdminUserDetailResponse from(User user) {
        return new AdminUserDetailResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCoin(),
                user.getBlackholedAt(),
                user.getRole()
        );
    }
}