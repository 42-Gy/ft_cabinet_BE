package com.gyeongsan.cabinet.domain.kakaonotify.model;

/** 카카오 호출이 그 밖의 이유로 실패했다(네트워크, 5xx, 해석 실패 등). 일시적일 수 있으니 동의를 해지 처리하지 않는다. */
public class KakaoNotificationException extends RuntimeException {

    public KakaoNotificationException(String message) {
        super(message);
    }

    public KakaoNotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
