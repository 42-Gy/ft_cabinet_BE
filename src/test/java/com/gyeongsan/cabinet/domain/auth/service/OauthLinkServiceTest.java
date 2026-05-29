package com.gyeongsan.cabinet.domain.auth.service;

import com.gyeongsan.cabinet.domain.auth.OauthLink;
import com.gyeongsan.cabinet.domain.auth.dto.OAuthUserInfo;
import com.gyeongsan.cabinet.domain.auth.port.out.OAuthApiClientPort;
import com.gyeongsan.cabinet.domain.auth.port.out.OauthLinkRepositoryPort;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import com.gyeongsan.cabinet.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OauthLinkServiceTest {

    @Mock
    private OauthLinkRepositoryPort oauthLinkRepository;

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private OAuthApiClientPort kakaoApiClient;

    @Mock
    private OAuthApiClientPort googleApiClient;

    private OauthLinkService oauthLinkService;

    @BeforeEach
    void setUp() {
        lenient().when(kakaoApiClient.supports("kakao")).thenReturn(true);
        lenient().when(googleApiClient.supports("google")).thenReturn(true);

        oauthLinkService = new OauthLinkService(
                oauthLinkRepository,
                userRepository,
                List.of(kakaoApiClient, googleApiClient)
        );
    }

    @Test
    @DisplayName("성공적으로 카카오 계정을 연동한다")
    void linkKakaoAccount_success() {
        // given
        Long userId = 1L;
        String authCode = "auth-code";
        OAuthUserInfo oauthInfo = new OAuthUserInfo("12345678", "kakao@example.com");
        User user = mock(User.class);

        given(kakaoApiClient.getOAuthUserInfo(authCode)).willReturn(oauthInfo);
        given(oauthLinkRepository.existsByProviderAndProviderId("kakao", "12345678")).willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(oauthLinkRepository.existsByUserAndProvider(user, "kakao")).willReturn(false);

        // when
        oauthLinkService.linkAccount(userId, "kakao", authCode);

        // then
        then(oauthLinkRepository).should(times(1)).save(any(OauthLink.class));
    }

    @Test
    @DisplayName("성공적으로 구글 계정을 연동한다")
    void linkGoogleAccount_success() {
        // given
        Long userId = 1L;
        String authCode = "auth-code";
        OAuthUserInfo oauthInfo = new OAuthUserInfo("google-sub-id", "google@example.com");
        User user = mock(User.class);

        given(googleApiClient.getOAuthUserInfo(authCode)).willReturn(oauthInfo);
        given(oauthLinkRepository.existsByProviderAndProviderId("google", "google-sub-id")).willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(oauthLinkRepository.existsByUserAndProvider(user, "google")).willReturn(false);

        // when
        oauthLinkService.linkAccount(userId, "google", authCode);

        // then
        then(oauthLinkRepository).should(times(1)).save(any(OauthLink.class));
    }

    @Test
    @DisplayName("이미 다른 42 계정에 연동된 소셜 ID인 경우 예외를 던진다")
    void linkAccount_alreadyLinkedToOther() {
        // given
        Long userId = 1L;
        String authCode = "auth-code";
        OAuthUserInfo oauthInfo = new OAuthUserInfo("12345678", "kakao@example.com");

        given(kakaoApiClient.getOAuthUserInfo(authCode)).willReturn(oauthInfo);
        given(oauthLinkRepository.existsByProviderAndProviderId("kakao", "12345678")).willReturn(true);

        // when & then
        ServiceException exception = assertThrows(ServiceException.class, () ->
                oauthLinkService.linkAccount(userId, "kakao", authCode)
        );

        assertEquals(ErrorCode.OAUTH_ALREADY_LINKED, exception.getErrorCode());
        then(oauthLinkRepository).should(never()).save(any(OauthLink.class));
    }

    @Test
    @DisplayName("현재 유저가 이미 해당 소셜 연동을 완료한 경우 예외를 던진다")
    void linkAccount_alreadyLinkedByUser() {
        // given
        Long userId = 1L;
        String authCode = "auth-code";
        OAuthUserInfo oauthInfo = new OAuthUserInfo("12345678", "kakao@example.com");
        User user = mock(User.class);

        given(kakaoApiClient.getOAuthUserInfo(authCode)).willReturn(oauthInfo);
        given(oauthLinkRepository.existsByProviderAndProviderId("kakao", "12345678")).willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(oauthLinkRepository.existsByUserAndProvider(user, "kakao")).willReturn(true);

        // when & then
        ServiceException exception = assertThrows(ServiceException.class, () ->
                oauthLinkService.linkAccount(userId, "kakao", authCode)
        );

        assertEquals(ErrorCode.OAUTH_ALREADY_LINKED_BY_USER, exception.getErrorCode());
        then(oauthLinkRepository).should(never()).save(any(OauthLink.class));
    }
}
