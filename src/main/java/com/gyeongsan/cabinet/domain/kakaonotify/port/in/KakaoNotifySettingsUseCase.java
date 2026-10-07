package com.gyeongsan.cabinet.domain.kakaonotify.port.in;

import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoNotifyStatus;

public interface KakaoNotifySettingsUseCase {

    KakaoNotifyStatus getStatus(Long userId);

    /** 알림 스위치를 바꾼다. 켜려면 유효한 동의가 있어야 한다(끄는 것은 항상 가능). */
    KakaoNotifyStatus setAlarm(Long userId, boolean enabled);
}
