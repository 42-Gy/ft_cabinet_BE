package com.gyeongsan.cabinet.domain.user.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {
    USER("ROLE_USER", "일반 사용자"),
    ADMIN("ROLE_ADMIN", "관리자"),
    MASTER("ROLE_MASTER", "최고 관리자"),
    BANNED("ROLE_BANNED", "이용 정지 사용자");

    private final String key;
    private final String title;
}
