package com.gyeongsan.cabinet.domain.kakaonotify.port.in;

import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoNotifyStatus;

public interface GrantKakaoNotifyConsentUseCase {

    /** 프론트가 scope=talk_message 로 받은 인가 코드로 알림 동의를 등록한다. 로그인용으로 연동해 둔 카카오 계정과 같은 계정이어야 한다. */
    KakaoNotifyStatus grantConsent(Long userId, String authorizationCode);
}
