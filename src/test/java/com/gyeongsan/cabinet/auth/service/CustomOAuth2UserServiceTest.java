package com.gyeongsan.cabinet.auth.service;

import com.gyeongsan.cabinet.domain.auth.OauthLink;
import com.gyeongsan.cabinet.domain.auth.port.out.OauthLinkRepositoryPort;
import com.gyeongsan.cabinet.item.repository.ItemHistoryRepository;
import com.gyeongsan.cabinet.item.repository.ItemRepository;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.repository.BannedUserRepository;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemHistoryRepository itemHistoryRepository;

    @Mock
    private BannedUserRepository bannedUserRepository;

    @Mock
    private OauthLinkRepositoryPort oauthLinkRepository;

    private CustomOAuth2UserService userService;

    @BeforeEach
    void setUp() {
        userService = new CustomOAuth2UserService(
                userRepository, itemRepository, itemHistoryRepository, bannedUserRepository, oauthLinkRepository
        );
    }

    private OAuth2UserRequest createOAuth2UserRequest(String registrationId, String userNameAttributeName) {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId(registrationId)
                .clientId("client-id")
                .tokenUri("https://token-uri")
                .authorizationUri("https://auth-uri")
                .userInfoUri("https://user-info-uri")
                .userNameAttributeName(userNameAttributeName)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://redirect-uri")
                .build();
        return new OAuth2UserRequest(clientRegistration, mock(OAuth2AccessToken.class));
    }

    @Test
    @DisplayName("카카오 로그인 시 연동 정보가 정상적으로 있으면 DefaultOAuth2User에 42 intraId가 login 키로 주입된다")
    void loadUser_kakaoSuccess() {
        // given
        OAuth2UserRequest request = createOAuth2UserRequest("kakao", "id");
        Map<String, Object> kakaoAttributes = Map.of("id", 98765432L, "connected_at", "2026-05-29T15:48:43Z");

        User user = mock(User.class);
        given(user.getName()).willReturn("juykim");

        OauthLink link = mock(OauthLink.class);
        given(link.getUser()).willReturn(user);

        given(oauthLinkRepository.findByProviderAndProviderId("kakao", "98765432")).willReturn(Optional.of(link));
        given(bannedUserRepository.existsByIntraId("juykim")).willReturn(false);

        // when
        OAuth2User result = userService.handleSocialLogin(request, kakaoAttributes, "kakao");

        // then
        assertEquals("98765432", result.getName());
        assertEquals("juykim", result.getAttributes().get("login"));
    }

    @Test
    @DisplayName("구글 로그인 시 연동 정보가 정상적으로 있으면 DefaultOAuth2User에 42 intraId가 login 키로 주입된다")
    void loadUser_googleSuccess() {
        // given
        OAuth2UserRequest request = createOAuth2UserRequest("google", "sub");
        Map<String, Object> googleAttributes = Map.of("sub", "google-sub-12345", "email", "google@example.com");

        User user = mock(User.class);
        given(user.getName()).willReturn("juykim");

        OauthLink link = mock(OauthLink.class);
        given(link.getUser()).willReturn(user);

        given(oauthLinkRepository.findByProviderAndProviderId("google", "google-sub-12345")).willReturn(Optional.of(link));
        given(bannedUserRepository.existsByIntraId("juykim")).willReturn(false);

        // when
        OAuth2User result = userService.handleSocialLogin(request, googleAttributes, "google");

        // then
        assertEquals("google-sub-12345", result.getName());
        assertEquals("juykim", result.getAttributes().get("login"));
    }

    @Test
    @DisplayName("카카오 로그인 시 연동 정보가 없으면 예외가 발생한다")
    void loadUser_kakaoNotLinked() {
        // given
        OAuth2UserRequest request = createOAuth2UserRequest("kakao", "id");
        Map<String, Object> kakaoAttributes = Map.of("id", 98765432L);

        given(oauthLinkRepository.findByProviderAndProviderId("kakao", "98765432")).willReturn(Optional.empty());

        // when & then
        OAuth2AuthenticationException exception = assertThrows(OAuth2AuthenticationException.class, () ->
                userService.handleSocialLogin(request, kakaoAttributes, "kakao")
        );

        assertEquals("먼저 42 인트라로 로그인하여 kakao 계정을 연동해 주세요.", exception.getError().getErrorCode());
    }

    @Test
    @DisplayName("구글 로그인 시 연동 정보가 없으면 예외가 발생한다")
    void loadUser_googleNotLinked() {
        // given
        OAuth2UserRequest request = createOAuth2UserRequest("google", "sub");
        Map<String, Object> googleAttributes = Map.of("sub", "google-sub-12345");

        given(oauthLinkRepository.findByProviderAndProviderId("google", "google-sub-12345")).willReturn(Optional.empty());

        // when & then
        OAuth2AuthenticationException exception = assertThrows(OAuth2AuthenticationException.class, () ->
                userService.handleSocialLogin(request, googleAttributes, "google")
        );

        assertEquals("먼저 42 인트라로 로그인하여 google 계정을 연동해 주세요.", exception.getError().getErrorCode());
    }

    @Test
    @DisplayName("구글 로그인 시 연동된 유저가 정지 상태이면 예외가 발생한다")
    void loadUser_googleBannedUser() {
        // given
        OAuth2UserRequest request = createOAuth2UserRequest("google", "sub");
        Map<String, Object> googleAttributes = Map.of("sub", "google-sub-12345");

        User user = mock(User.class);
        given(user.getName()).willReturn("bannedUser");

        OauthLink link = mock(OauthLink.class);
        given(link.getUser()).willReturn(user);

        given(oauthLinkRepository.findByProviderAndProviderId("google", "google-sub-12345")).willReturn(Optional.of(link));
        given(bannedUserRepository.existsByIntraId("bannedUser")).willReturn(true);

        // when & then
        OAuth2AuthenticationException exception = assertThrows(OAuth2AuthenticationException.class, () ->
                userService.handleSocialLogin(request, googleAttributes, "google")
        );

        assertEquals("서비스 이용이 제한된 유저입니다. 관리자에게 문의하세요.", exception.getError().getErrorCode());
    }
}
