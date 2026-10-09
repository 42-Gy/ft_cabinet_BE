package com.gyeongsan.cabinet.domain.kakaonotify.model;

/** 메시지 발송이 동의 항목 부족(카카오 코드 -402)으로 거절됐다. talk_message 동의가 없거나 해지된 상태다. */
public class KakaoInsufficientScopeException extends RuntimeException {

    public KakaoInsufficientScopeException(String message) {
        super(message);
    }
}
