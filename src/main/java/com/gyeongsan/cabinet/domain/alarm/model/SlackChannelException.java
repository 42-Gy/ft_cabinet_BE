package com.gyeongsan.cabinet.domain.alarm.model;

/** 슬랙 채널 조회 실패. 호출하는 쪽은 이번 주기를 건너뛰고 다음 주기에 다시 시도한다. */
public class SlackChannelException extends RuntimeException {

    public SlackChannelException(String message) {
        super(message);
    }

    public SlackChannelException(String message, Throwable cause) {
        super(message, cause);
    }
}
