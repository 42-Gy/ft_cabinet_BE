package com.gyeongsan.cabinet.domain.kakaonotify.model;

/**
 * 카카오가 {@code invalid_grant} 로 거절했다. 인가 코드를 교환할 때는 "코드가 만료됐거나 이미 썼다"는 뜻이고, refresh_token 으로 갱신할 때는
 * "유저가 동의를 해지했거나 토큰이 만료됐다"는 뜻이다.
 */
public class KakaoInvalidGrantException extends RuntimeException {

    public KakaoInvalidGrantException(String message) {
        super(message);
    }
}
