package com.gyeongsan.cabinet.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {

    USER("ROLE_USER", "일반 사용자"),
    ADMIN("ROLE_ADMIN", "관리자"),
    MASTER("ROLE_MASTER", "최고 관리자");

    private final String key;
    private final String title;
}
