package com.gyeongsan.cabinet.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.gyeongsan.cabinet.adapter.out.persistence.user.UserRepository;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenProviderTest {

    private UserRepository userRepository;
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        jwtTokenProvider = new JwtTokenProvider(userRepository);
        ReflectionTestUtils.setField(
                jwtTokenProvider, "secretKey", "test-secret-key-test-secret-key-test-secret-key");
        ReflectionTestUtils.setField(jwtTokenProvider, "tokenValidTime", 60_000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenValidTime", 60_000L);
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("getAuthentication: access token이면 인증 정보를 만든다")
    void getAuthentication_accessToken_success() {
        User user = User.builder().id(1L).name("tester").role(UserRole.USER).build();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        Authentication authentication =
                jwtTokenProvider.getAuthentication(
                        jwtTokenProvider.createToken(1L, "tester", "USER"));

        assertThat(authentication.getName()).isEqualTo("tester");
    }

    @Test
    @DisplayName("getAuthentication: refresh token으로는 인증할 수 없다")
    void getAuthentication_refreshToken_rejected() {
        String refreshToken = jwtTokenProvider.createRefreshToken(1L);

        assertThat(jwtTokenProvider.validateToken(refreshToken)).isTrue();
        assertThatThrownBy(() -> jwtTokenProvider.getAuthentication(refreshToken))
                .isInstanceOf(IllegalArgumentException.class);
        verify(userRepository, never()).findById(any());
    }
}
