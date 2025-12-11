package com.gyeongsan.cabinet.auth.oauth;

import com.gyeongsan.cabinet.auth.jwt.JwtTokenProvider;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.domain.UserRole;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Log4j2
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String intraId = (String) oAuth2User.getAttributes().get("login"); // 42 아이디 추출

        log.info("🎉 로그인 성공! 토큰 발급 시작: {}", intraId);

        // 1. 유저 정보 조회 (없으면 가입, 있으면 조회) -> 이미 Service에서 했으므로 조회만
        User user = userRepository.findByName(intraId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        // 2. 토큰 생성 (Access Token)
        String accessToken = jwtTokenProvider.createToken(user.getId(), user.getName(), user.getRole().name());

        log.info("🎫 JWT 토큰 발급 완료: {}", accessToken);

        // 3. 토큰을 가지고 메인 페이지로 리다이렉트 (쿼리 파라미터로 전달)
        // 실제 배포 시에는 쿠키(Cookie)에 담거나 프론트엔드 URL로 보내야 합니다.
        // 지금은 테스트를 위해 localhost:8080/?token=... 형태로 보냅니다.
        String targetUrl = UriComponentsBuilder.fromUriString("/")
                .queryParam("token", accessToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}