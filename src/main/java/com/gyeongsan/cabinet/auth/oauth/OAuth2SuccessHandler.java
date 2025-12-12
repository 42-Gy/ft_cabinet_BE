package com.gyeongsan.cabinet.auth.oauth;

import com.gyeongsan.cabinet.auth.jwt.JwtTokenProvider;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.StringRedisTemplate; // 👈 추가
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit; // 👈 추가

@Component
@RequiredArgsConstructor
@Log4j2
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate; // 👈 Redis 주입

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String intraId = (String) oAuth2User.getAttributes().get("login");

        log.info("🎉 로그인 성공! 토큰 발급 시작: {}", intraId);

        User user = userRepository.findByName(intraId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        // 1. Access Token 발급 (30분)
        String accessToken = jwtTokenProvider.createToken(user.getId(), user.getName(), user.getRole().name());

        // 2. [Ver 3.5] Refresh Token 발급 (14일)
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // 3. [Ver 3.5] Redis에 Refresh Token 저장 (Key: "RT:userId", Value: refreshToken, Timeout: 14일)
        //
        redisTemplate.opsForValue().set(
                "RT:" + user.getId(),
                refreshToken,
                14,
                TimeUnit.DAYS
        );
        log.info("💾 Refresh Token Redis 저장 완료: {}", user.getId());

        // 4. [Ver 3.5] Refresh Token을 HttpOnly 쿠키로 클라이언트에 전달
        response.addCookie(createCookie("refresh_token", refreshToken));

        // 5. Access Token은 URL 파라미터로 전달 (기존 방식)
        log.info("🎫 Access Token 발급 완료: {}", accessToken);

        String targetUrl = UriComponentsBuilder.fromUriString("/")
                .queryParam("token", accessToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    // 쿠키 생성 헬퍼 메서드
    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(14 * 24 * 60 * 60); // 14일 (초 단위)
        cookie.setPath("/"); // 모든 경로에서 접근 가능
        cookie.setHttpOnly(true); // 👈 중요: 자바스크립트로 접근 불가 (XSS 방지)
        // cookie.setSecure(true); // HTTPS 적용 시 주석 해제 필수
        return cookie;
    }
}