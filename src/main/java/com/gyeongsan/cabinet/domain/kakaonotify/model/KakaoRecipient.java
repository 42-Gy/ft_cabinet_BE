package com.gyeongsan.cabinet.domain.kakaonotify.model;

/** 발송 대상 한 명. 동의가 유효하고 알림이 켜진 유저다. 토큰은 암호화된 상태 그대로다. */
public record KakaoRecipient(Long userId, String userName, String encryptedRefreshToken) {

    @Override
    public String toString() {
        return "KakaoRecipient[userId=" + userId + ", userName=" + userName + "]";
    }
}
