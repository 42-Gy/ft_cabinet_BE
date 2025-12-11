package com.gyeongsan.cabinet.auth.config;

import com.gyeongsan.cabinet.auth.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF 보안 기능 끄기 (개발용)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. 주소별 권한 설정
                .authorizeHttpRequests(auth -> auth

                        // 👇 [추가] 관리자 전용 경로 설정 (ADMIN 권한 필수)
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // 테스트용 주소 로그인 필수
                        .requestMatchers("/test/**").authenticated()

                        // 실제 서비스 API(/v4/...)도 로그인 필수!
                        .requestMatchers("/v4/**").authenticated()

                        // 나머지는 통과 (메인 페이지, 정적 리소스 등)
                        .anyRequest().permitAll()
                )

                // 3. 42 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        // 로그인 성공 시 메인 페이지("/")로 이동
                        .defaultSuccessUrl("/", true)
                        // 로그인 성공 후 유저 정보 처리 서비스 등록
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                );

        return http.build();
    }
}