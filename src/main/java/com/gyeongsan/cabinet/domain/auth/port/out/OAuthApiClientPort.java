package com.gyeongsan.cabinet.domain.auth.port.out;

import com.gyeongsan.cabinet.domain.auth.dto.OAuthUserInfo;

public interface OAuthApiClientPort {
    OAuthUserInfo getOAuthUserInfo(String authorizationCode);
    boolean supports(String provider);
}
