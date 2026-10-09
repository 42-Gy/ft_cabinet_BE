package com.gyeongsan.cabinet.domain.kakaonotify.model;

/**
 * 내 카카오 알림 상태.
 *
 * @param consented 카카오 talk_message 동의가 유효한가
 * @param alarmEnabled 알림 스위치가 켜져 있는가
 */
public record KakaoNotifyStatus(boolean consented, boolean alarmEnabled) {

    /** 실제로 알림이 가는 조건: 동의 유효 AND 스위치 켜짐. */
    public boolean receiving() {
        return consented && alarmEnabled;
    }
}
