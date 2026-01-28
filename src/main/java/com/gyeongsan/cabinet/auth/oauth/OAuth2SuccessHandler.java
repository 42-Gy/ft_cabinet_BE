package com.gyeongsan.cabinet.auth.oauth;

import com.gyeongsan.cabinet.auth.jwt.JwtTokenProvider;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Log4j2
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

        private final JwtTokenProvider jwtTokenProvider;
        private final UserRepository userRepository;
        private final StringRedisTemplate redisTemplate;

        private String frontendUrl = "https://subak.site";

        @Value("${app.auth.cookie-secure:false}")
        private boolean isCookieSecure;

        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                        Authentication authentication)
                        throws IOException, ServletException {

                OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
                String intraId = (String) oAuth2User.getAttributes().get("login");

                log.info("🎉 로그인 성공! 토큰 발급 시작: {}", intraId);

                User user = userRepository.findByName(intraId)
                                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

                String accessToken = jwtTokenProvider.createToken(user.getId(), user.getName(), user.getRole().name());
                String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

                redisTemplate.opsForValue().set(
                                "RT:" + user.getId(),
                                refreshToken,
                                14,
                                TimeUnit.DAYS);

                log.info("💾 Refresh Token Redis 저장 완료: {}", user.getId());

                ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token", refreshToken)
                                .maxAge(14 * 24 * 60 * 60)
                                .path("/")

                                .secure(true)
                                .sameSite("None")
                                .httpOnly(true)
                                .build();

                response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

                ResponseCookie accessTokenCookie = ResponseCookie.from("access_token", accessToken)
                                .path("/")

                                .secure(true)
                                .sameSite("None")
                                .httpOnly(true)
                                .build();

                response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

                log.info("🎫 Access Token 발급 및 쿠키 설정 완료");

                String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                                .path("/auth/callback")
                                .queryParam("access_token", accessToken)
                                .build()
                                .toUriString();
                getRedirectStrategy().sendRedirect(request, response, targetUrl);
        }
}
