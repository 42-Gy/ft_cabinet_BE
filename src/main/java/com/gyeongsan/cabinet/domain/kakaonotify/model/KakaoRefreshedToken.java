package com.gyeongsan.cabinet.domain.kakaonotify.model;

/**
 * refresh_token 으로 새로 받은 access_token. 카카오는 refresh_token 의 남은 기간이 짧을 때만 새 refresh_token 을 함께
 * 준다(그렇지 않으면 null).
 */
public record KakaoRefreshedToken(String accessToken, String newRefreshToken) {

    @Override
    public String toString() {
        return "KakaoRefreshedToken[rotated=" + (newRefreshToken != null) + "]";
    }
}
