package com.gyeongsan.cabinet.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 API 경로에 대해
                .allowedOriginPatterns("*") // 모든 출처 허용 (배포 시 프론트 주소로 변경 권장)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS") // 허용할 HTTP 메서드
                .allowCredentials(true) // 쿠키(세션) 인증 요청 허용
                .maxAge(3000); // 설정 캐싱 시간
    }
}