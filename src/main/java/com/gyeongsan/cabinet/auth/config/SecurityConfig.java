package com.gyeongsan.cabinet.auth.config;

import com.gyeongsan.cabinet.auth.jwt.JwtAuthenticationFilter; // 👈 추가
import com.gyeongsan.cabinet.auth.jwt.JwtTokenProvider;         // 👈 추가
import com.gyeongsan.cabinet.auth.oauth.OAuth2SuccessHandler;   // 👈 추가
import com.gyeongsan.cabinet.auth.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy; // 👈 추가
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // 👈 추가

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtTokenProvider jwtTokenProvider;          // 👈 [Ver 3.0] 주입
    private final OAuth2SuccessHandler oAuth2SuccessHandler;  // 👈 [Ver 3.0] 주입

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF 비활성화 (JWT 사용 시 불필요)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. [Ver 3.0 핵심] 세션을 사용하지 않음 (Stateless 설정)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 3. 주소별 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/v4/**").authenticated() // /v4/로 시작하는 건 인증 필요
                        .anyRequest().permitAll()                  // 나머지는 통과 (로그인, Actuator 등)
                )

                // 4. [Ver 3.0 핵심] JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 배치
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)

                // 5. 42 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        // 👇 [Ver 3.0 핵심] 로그인 성공 시 핸들러 연결 (여기서 토큰 발급!)
                        .successHandler(oAuth2SuccessHandler)
                );

        return http.build();
    }
}