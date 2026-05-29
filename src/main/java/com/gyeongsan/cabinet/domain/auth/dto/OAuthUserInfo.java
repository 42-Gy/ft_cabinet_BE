package com.gyeongsan.cabinet.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OAuthUserInfo {
    private String providerId;
    private String email;
}
