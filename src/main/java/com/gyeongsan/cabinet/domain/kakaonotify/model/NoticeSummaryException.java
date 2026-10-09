package com.gyeongsan.cabinet.domain.kakaonotify.model;

/** 공지 요약 호출 실패(시간 초과, HTTP 오류, 응답 해석 실패 등). 메시지에 본문·키를 담지 않는다. */
public class NoticeSummaryException extends RuntimeException {

    public NoticeSummaryException(String message) {
        super(message);
    }

    public NoticeSummaryException(String message, Throwable cause) {
        super(message, cause);
    }
}
