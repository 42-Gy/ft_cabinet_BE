package com.gyeongsan.cabinet.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gyeongsan.cabinet.adapter.out.persistence.item.ItemHistoryRepository;
import com.gyeongsan.cabinet.adapter.out.persistence.item.ItemRepository;
import com.gyeongsan.cabinet.adapter.out.persistence.user.BannedUserRepository;
import com.gyeongsan.cabinet.adapter.out.persistence.user.UserRepository;
import com.gyeongsan.cabinet.domain.auth.port.out.OauthLinkRepositoryPort;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.support.FtFixtures;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.util.ReflectionTestUtils;

/** 42 로그인 시 본과정 grade 를 읽어 USER.FT_GRADE 로 저장/갱신하는 흐름. */
class CustomOAuth2UserServiceGradeTest {

    private UserRepository userRepository;
    private CustomOAuth2UserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        BannedUserRepository bannedUserRepository = mock(BannedUserRepository.class);
        service =
                new CustomOAuth2UserService(
                        userRepository,
                        mock(ItemRepository.class),
                        mock(ItemHistoryRepository.class),
                        bannedUserRepository,
                        mock(OauthLinkRepositoryPort.class));
        ReflectionTestUtils.setField(service, "allowedEmailDomain", "@student.42gyeongsan.kr");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private OAuth2UserRequest request() {
        ClientRegistration registration =
                ClientRegistration.withRegistrationId("42")
                        .clientId("client-id")
                        .tokenUri("https://token-uri")
                        .authorizationUri("https://auth-uri")
                        .userInfoUri("https://user-info-uri")
                        .userNameAttributeName("login")
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .redirectUri("https://redirect-uri")
                        .build();
        return new OAuth2UserRequest(registration, mock(OAuth2AccessToken.class));
    }

    private Map<String, Object> attributes(String fixture) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("login", "test-user");
        attributes.put("email", "test-user@student.42gyeongsan.kr");
        attributes.putAll(FtFixtures.asAttributes(fixture));
        return attributes;
    }

    private User login(Optional<User> existing, Map<String, Object> attributes) {
        when(userRepository.findByName("test-user")).thenReturn(existing);
        service.handleFtLogin(request(), attributes);
        org.mockito.ArgumentCaptor<User> saved = org.mockito.ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(userRepository, org.mockito.Mockito.atLeastOnce())
                .save(saved.capture());
        return saved.getValue();
    }

    @Test
    @DisplayName("신규 가입: 트센 응답이면 grade 가 Transcender 로 저장되고 피시너가 아니다")
    void newTranscender() {
        User user = login(Optional.empty(), attributes(FtFixtures.TRANSCENDER));

        assertThat(user.getFtGrade()).isEqualTo("Transcender");
        assertThat(user.isTranscender()).isTrue();
        assertThat(user.isPisciner()).isFalse();
    }

    @Test
    @DisplayName("신규 가입: Cadet 응답이면 일반 사용자로 저장된다")
    void newCadet() {
        User user = login(Optional.empty(), attributes(FtFixtures.CADET));

        assertThat(user.getFtGrade()).isEqualTo("Cadet");
        assertThat(user.isTranscender()).isFalse();
    }

    @Test
    @DisplayName("기존 사용자가 Cadet 에서 Transcender 로 바뀌면 로그인 때 갱신된다")
    void existingUserIsUpdated() {
        User existing = User.of("test-user", "test-user@student.42gyeongsan.kr", UserRole.USER);
        existing.updateFtGrade("Cadet");

        User user = login(Optional.of(existing), attributes(FtFixtures.TRANSCENDER));

        assertThat(user.getFtGrade()).isEqualTo("Transcender");
    }

    @Test
    @DisplayName("cursus_users 를 해석하지 못한 로그인은 저장된 grade 를 지우지 않는다")
    void unparsedKeepsStoredGrade() {
        User existing = User.of("test-user", "test-user@student.42gyeongsan.kr", UserRole.USER);
        existing.updateFtGrade("Transcender");
        Map<String, Object> attributes = attributes(FtFixtures.CADET);
        attributes.remove("cursus_users");

        User user = login(Optional.of(existing), attributes);

        assertThat(user.getFtGrade()).isEqualTo("Transcender");
    }

    @Test
    @DisplayName("합성 데이터: 피시너만 있는 응답이면 grade 는 null 로 갱신되고 피시너로 표시된다")
    void pisciner() {
        Map<String, Object> attributes = attributes(FtFixtures.CADET);
        attributes.put(
                "cursus_users",
                java.util.List.of(
                        Map.of(
                                "grade",
                                "Pisciner",
                                "cursus",
                                Map.of("id", 9, "slug", "c-piscine"))));

        User user = login(Optional.empty(), attributes);

        assertThat(user.getFtGrade()).isNull();
        assertThat(user.isPisciner()).isTrue();
        assertThat(user.isTranscender()).isFalse();
    }

    @Test
    @DisplayName("합성 데이터: 형식이 잘못된 grade 는 저장하지 않는다")
    void rejectedGradeIsNotStored() {
        Map<String, Object> attributes = attributes(FtFixtures.CADET);
        attributes.put(
                "cursus_users",
                java.util.List.of(Map.of("grade", "x".repeat(40), "cursus", Map.of("id", 21))));
        User existing = User.of("test-user", "test-user@student.42gyeongsan.kr", UserRole.USER);
        existing.updateFtGrade("Cadet");

        User user = login(Optional.of(existing), attributes);

        assertThat(user.getFtGrade()).isEqualTo("Cadet");
    }
}
