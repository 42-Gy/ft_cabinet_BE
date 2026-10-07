package com.gyeongsan.cabinet.application.kakaonotify;

import com.gyeongsan.cabinet.domain.auth.port.out.LinkRedirectUriPort;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoGrant;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoInvalidGrantException;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoNotificationException;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoNotifyStatus;
import com.gyeongsan.cabinet.domain.kakaonotify.port.in.GrantKakaoNotifyConsentUseCase;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.KakaoConsentRepositoryPort;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.KakaoLoginLinkPort;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.KakaoNotificationPort;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.TokenCipherPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * talk_message 동의 등록. 로그인용 연동({@code OauthLinkService.linkAccount})과는 별개의 경로다.
 *
 * <p>순서: ① 로그인용 카카오 연동 확인(없으면 거부) → ② 인가 코드 교환(카카오 호출, 트랜잭션 밖) → ③ 돌아온 카카오 회원 번호가 연동된 계정과 같은지
 * 확인(다르면 거부, 받은 토큰은 버린다) → ④ talk_message 범위 확인 → ⑤ refresh_token 을 암호화해 저장.
 *
 * <p>인가 코드 교환 때의 {@code redirect_uri} 는 계정 연동과 같은 값(FRONTEND_URL 파생)을 쓴다. 프론트는 동의 화면을 띄울 때 연동 때와 같은
 * redirect_uri 를 써야 한다.
 */
@Log4j2
@RequiredArgsConstructor
public class KakaoNotifyConsentService implements GrantKakaoNotifyConsentUseCase {

    static final String REQUIRED_SCOPE = "talk_message";

    private final KakaoLoginLinkPort loginLinkPort;
    private final LinkRedirectUriPort redirectUriPort;
    private final KakaoNotificationPort kakao;
    private final KakaoConsentRepositoryPort consentRepository;
    private final TokenCipherPort cipher;
    private final Clock clock;

    @Override
    public KakaoNotifyStatus grantConsent(Long userId, String authorizationCode) {
        if (authorizationCode == null || authorizationCode.isBlank()) {
            throw new ServiceException(ErrorCode.KAKAO_NOTIFY_CODE_INVALID);
        }

        String linkedProviderId =
                loginLinkPort
                        .findKakaoProviderId(userId)
                        .orElseThrow(
                                () -> new ServiceException(ErrorCode.KAKAO_NOTIFY_LINK_REQUIRED));

        KakaoGrant grant;
        try {
            grant =
                    kakao.exchangeAuthorizationCode(
                            authorizationCode.trim(), redirectUriPort.getLinkRedirectUri("kakao"));
        } catch (KakaoInvalidGrantException e) {
            throw new ServiceException(ErrorCode.KAKAO_NOTIFY_CODE_INVALID);
        } catch (KakaoNotificationException e) {
            log.warn("[KakaoNotify] 인가 코드 교환 실패 - userId: {}, 원인: {}", userId, e.getMessage());
            throw new ServiceException(ErrorCode.KAKAO_NOTIFY_UNAVAILABLE);
        }

        if (!linkedProviderId.equals(grant.providerId())) {
            // 다른 카카오 계정의 토큰이다. 저장하지 않고 버린다.
            log.warn("[KakaoNotify] 연동된 카카오 계정과 다른 계정으로 동의 시도 - userId: {}", userId);
            throw new ServiceException(ErrorCode.KAKAO_NOTIFY_ACCOUNT_MISMATCH);
        }
        if (!grant.scopes().contains(REQUIRED_SCOPE)) {
            throw new ServiceException(ErrorCode.KAKAO_NOTIFY_SCOPE_MISSING);
        }

        String encrypted =
                cipher.encrypt(grant.refreshToken(), SlackNoticeForwardService.context(userId));
        boolean activated =
                consentRepository.grant(
                        userId,
                        encrypted,
                        String.join(" ", new TreeSet<>(grant.scopes())),
                        LocalDateTime.now(clock));

        log.info("[KakaoNotify] 알림 동의 등록 - userId: {}, 알림 스위치 새로 켬: {}", userId, activated);
        return consentRepository.findStatus(userId);
    }
}
