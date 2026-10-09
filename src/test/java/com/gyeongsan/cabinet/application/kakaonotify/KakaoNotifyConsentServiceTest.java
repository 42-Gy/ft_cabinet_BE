package com.gyeongsan.cabinet.application.kakaonotify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gyeongsan.cabinet.adapter.out.crypto.AesGcmTokenCipher;
import com.gyeongsan.cabinet.domain.auth.port.out.LinkRedirectUriPort;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoGrant;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoInvalidGrantException;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoNotificationException;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoNotifyStatus;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.KakaoConsentRepositoryPort;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.KakaoLoginLinkPort;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.KakaoNotificationPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class KakaoNotifyConsentServiceTest {

    private final KakaoLoginLinkPort loginLink = mock(KakaoLoginLinkPort.class);
    private final LinkRedirectUriPort redirectUri = mock(LinkRedirectUriPort.class);
    private final KakaoNotificationPort kakao = mock(KakaoNotificationPort.class);
    private final KakaoConsentRepositoryPort consents = mock(KakaoConsentRepositoryPort.class);
    private AesGcmTokenCipher cipher;
    private KakaoNotifyConsentService service;

    @BeforeEach
    void setUp() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        cipher = new AesGcmTokenCipher(Base64.getEncoder().encodeToString(key));
        service =
                new KakaoNotifyConsentService(
                        loginLink,
                        redirectUri,
                        kakao,
                        consents,
                        cipher,
                        Clock.fixed(Instant.parse("2026-10-07T00:00:00Z"), ZoneOffset.UTC));
        when(redirectUri.getLinkRedirectUri("kakao"))
                .thenReturn("https://front.example/link/kakao");
        when(consents.findStatus(7L)).thenReturn(new KakaoNotifyStatus(true, true));
    }

    private static KakaoGrant grant(String providerId, String... scopes) {
        return new KakaoGrant(providerId, "refresh-plain", Set.of(scopes));
    }

    @Test
    @DisplayName("연동된 카카오 계정과 같고 talk_message 를 받았다면 refresh_token 을 암호화해 저장한다")
    void storesEncryptedToken() {
        when(loginLink.findKakaoProviderId(7L)).thenReturn(Optional.of("999"));
        when(kakao.exchangeAuthorizationCode("code-1", "https://front.example/link/kakao"))
                .thenReturn(grant("999", "talk_message", "profile_nickname"));
        when(consents.grant(eq(7L), anyString(), anyString(), any())).thenReturn(true);

        KakaoNotifyStatus status = service.grantConsent(7L, " code-1 ");

        assertThat(status.receiving()).isTrue();
        ArgumentCaptor<String> stored = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> scope = ArgumentCaptor.forClass(String.class);
        verify(consents).grant(eq(7L), stored.capture(), scope.capture(), any());
        assertThat(stored.getValue()).startsWith("v1.").doesNotContain("refresh-plain");
        assertThat(cipher.decrypt(stored.getValue(), "kakao-notify:7")).isEqualTo("refresh-plain");
        assertThat(scope.getValue()).isEqualTo("profile_nickname talk_message");
    }

    @Test
    @DisplayName("로그인용 카카오 연동이 없으면 거부하고, 인가 코드는 카카오에 보내지도 않는다(코드를 낭비하지 않음)")
    void requiresLoginLinkFirst() {
        when(loginLink.findKakaoProviderId(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.grantConsent(7L, "code"))
                .isInstanceOfSatisfying(
                        ServiceException.class,
                        e ->
                                assertThat(e.getErrorCode())
                                        .isEqualTo(ErrorCode.KAKAO_NOTIFY_LINK_REQUIRED));
        verify(kakao, never()).exchangeAuthorizationCode(anyString(), anyString());
        verify(consents, never()).grant(any(), any(), any(), any());
    }

    @Test
    @DisplayName("연동한 계정과 다른 카카오 계정으로 동의하면 거부하고 아무것도 저장하지 않는다")
    void rejectsDifferentKakaoAccount() {
        when(loginLink.findKakaoProviderId(7L)).thenReturn(Optional.of("999"));
        when(kakao.exchangeAuthorizationCode(anyString(), anyString()))
                .thenReturn(grant("111", "talk_message"));

        assertThatThrownBy(() -> service.grantConsent(7L, "code"))
                .isInstanceOfSatisfying(
                        ServiceException.class,
                        e ->
                                assertThat(e.getErrorCode())
                                        .isEqualTo(ErrorCode.KAKAO_NOTIFY_ACCOUNT_MISMATCH));
        verify(consents, never()).grant(any(), any(), any(), any());
    }

    @Test
    @DisplayName("talk_message 에 동의하지 않았으면 거부한다")
    void rejectsMissingScope() {
        when(loginLink.findKakaoProviderId(7L)).thenReturn(Optional.of("999"));
        when(kakao.exchangeAuthorizationCode(anyString(), anyString()))
                .thenReturn(grant("999", "profile_nickname"));

        assertThatThrownBy(() -> service.grantConsent(7L, "code"))
                .isInstanceOfSatisfying(
                        ServiceException.class,
                        e ->
                                assertThat(e.getErrorCode())
                                        .isEqualTo(ErrorCode.KAKAO_NOTIFY_SCOPE_MISSING));
        verify(consents, never()).grant(any(), any(), any(), any());
    }

    @Test
    @DisplayName("인가 코드가 만료·재사용(invalid_grant)이면 코드 오류로, 카카오 장애면 통신 오류로 알린다")
    void mapsKakaoErrors() {
        when(loginLink.findKakaoProviderId(7L)).thenReturn(Optional.of("999"));
        when(kakao.exchangeAuthorizationCode(eq("expired"), anyString()))
                .thenThrow(new KakaoInvalidGrantException("invalid_grant"));
        when(kakao.exchangeAuthorizationCode(eq("down"), anyString()))
                .thenThrow(new KakaoNotificationException("503"));

        assertThatThrownBy(() -> service.grantConsent(7L, "expired"))
                .isInstanceOfSatisfying(
                        ServiceException.class,
                        e ->
                                assertThat(e.getErrorCode())
                                        .isEqualTo(ErrorCode.KAKAO_NOTIFY_CODE_INVALID));
        assertThatThrownBy(() -> service.grantConsent(7L, "down"))
                .isInstanceOfSatisfying(
                        ServiceException.class,
                        e ->
                                assertThat(e.getErrorCode())
                                        .isEqualTo(ErrorCode.KAKAO_NOTIFY_UNAVAILABLE));
    }

    @Test
    @DisplayName("빈 인가 코드는 카카오를 부르기 전에 거부한다")
    void rejectsBlankCode() {
        assertThatThrownBy(() -> service.grantConsent(7L, "  "))
                .isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> service.grantConsent(7L, null))
                .isInstanceOf(ServiceException.class);
        verify(loginLink, never()).findKakaoProviderId(any());
    }
}
