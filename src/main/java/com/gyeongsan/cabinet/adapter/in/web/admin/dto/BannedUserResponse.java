package com.gyeongsan.cabinet.adapter.in.web.admin.dto;

import com.gyeongsan.cabinet.domain.user.model.BannedUser;
import java.time.LocalDateTime;

public record BannedUserResponse(Long id, String intraId, String reason, LocalDateTime bannedAt) {
    public static BannedUserResponse from(BannedUser bannedUser) {
        return new BannedUserResponse(
                bannedUser.getId(),
                bannedUser.getIntraId(),
                bannedUser.getReason(),
                bannedUser.getBannedAt());
    }
}
