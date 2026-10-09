package com.gyeongsan.cabinet.domain.kakaonotify.model;

/** 저장된 토큰을 복호화하지 못했다(키가 바뀌었거나 분실, 값 손상, 다른 유저의 값). 토큰 자체는 로그에 남기지 않는다. */
public class TokenDecryptionException extends RuntimeException {

    public TokenDecryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
