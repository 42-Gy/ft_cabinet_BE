package com.gyeongsan.cabinet.adapter.in.web.kakaonotify.dto;

import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoNotifyStatus;

/**
 * @param consented 카카오 talk_message 동의가 유효한가
 * @param alarmEnabled 알림 스위치
 * @param receiving 실제로 공지를 받는 상태인가(동의 유효 AND 스위치 켜짐)
 */
public record KakaoNotifyStatusResponse(
        boolean consented, boolean alarmEnabled, boolean receiving) {

    public static KakaoNotifyStatusResponse from(KakaoNotifyStatus status) {
        return new KakaoNotifyStatusResponse(
                status.consented(), status.alarmEnabled(), status.receiving());
    }
}
