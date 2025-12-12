package com.gyeongsan.cabinet.auth.controller;

import com.gyeongsan.cabinet.auth.jwt.JwtTokenProvider;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v4/auth")
@RequiredArgsConstructor
@Log4j2
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;

    @PostMapping("/reissue")
    public Map<String, String> reissue(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        log.info("🔄 토큰 재발급 요청 들어옴!");

        if (refreshToken == null) {
            throw new IllegalArgumentException("Refresh Token이 없습니다. 다시 로그인하세요.");
        }

        // 1. 토큰 유효성 검사
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Refresh Token이 만료되었거나 유효하지 않습니다.");
        }

        // 2. 토큰에서 유저 ID 추출
        String userId = jwtTokenProvider.parseClaims(refreshToken).getSubject();

        // 3. Redis에 저장된 토큰과 일치하는지 확인 (보안 검증)
        String savedToken = redisTemplate.opsForValue().get("RT:" + userId);
        if (savedToken == null || !savedToken.equals(refreshToken)) {
            log.warn("🚨 Redis 토큰 불일치! 탈취 가능성 있음. User: {}", userId);
            throw new IllegalArgumentException("토큰 정보가 일치하지 않습니다.");
        }

        // 4. 유저 정보 조회
        User user = userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new IllegalArgumentException("유저가 없습니다."));

        // 5. 새 Access Token 발급
        String newAccessToken = jwtTokenProvider.createToken(user.getId(), user.getName(), user.getRole().name());
        log.info("🎫 새 Access Token 발급 완료: {}", user.getName());

        // 6. 결과 반환
        Map<String, String> result = new HashMap<>();
        result.put("accessToken", newAccessToken);

        return result;
    }
}