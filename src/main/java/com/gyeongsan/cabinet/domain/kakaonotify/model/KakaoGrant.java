package com.gyeongsan.cabinet.domain.kakaonotify.model;

import java.util.Set;

/** 인가 코드를 토큰으로 교환한 결과. access_token 은 밖으로 내보내지 않는다. */
public record KakaoGrant(String providerId, String refreshToken, Set<String> scopes) {

    public KakaoGrant {
        scopes = scopes == null ? Set.of() : Set.copyOf(scopes);
    }

    @Override
    public String toString() {
        // 토큰이 로그에 찍히지 않게 한다.
        return "KakaoGrant[providerId=" + providerId + ", scopes=" + scopes + "]";
    }
}
