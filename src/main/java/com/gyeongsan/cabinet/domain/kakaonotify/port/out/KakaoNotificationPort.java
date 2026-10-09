package com.gyeongsan.cabinet.domain.kakaonotify.port.out;

import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoGrant;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoInsufficientScopeException;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoInvalidGrantException;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoMessage;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoNotificationException;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoRefreshedToken;

/**
 * 카카오 알림 발송에 필요한 외부 호출. 로그인용 OAuthApiClientPort(신원 확인만)와 일부러 분리했다.
 *
 * <p>모든 메서드는 토큰이나 인가 코드를 예외 메시지·로그에 남기지 않는다.
 */
public interface KakaoNotificationPort {

    /**
     * 인가 코드를 토큰으로 교환하고, 그 토큰의 카카오 회원 번호(providerId)를 확인한다.
     *
     * @throws KakaoInvalidGrantException 코드가 만료됐거나 이미 사용됨
     * @throws KakaoNotificationException 그 밖의 호출 실패
     */
    KakaoGrant exchangeAuthorizationCode(String authorizationCode, String redirectUri);

    /**
     * refresh_token 으로 access_token 을 새로 받는다.
     *
     * @throws KakaoInvalidGrantException 동의가 해지됐거나 refresh_token 이 유효하지 않음
     * @throws KakaoNotificationException 그 밖의 호출 실패(일시적일 수 있음)
     */
    KakaoRefreshedToken refreshAccessToken(String refreshToken);

    /**
     * 본인에게 카카오톡 메시지를 보낸다("나에게 보내기").
     *
     * @throws KakaoInsufficientScopeException talk_message 동의가 없거나 해지됨
     * @throws KakaoNotificationException 그 밖의 호출 실패
     */
    void sendToMe(String accessToken, KakaoMessage message);
}
