package com.gyeongsan.cabinet.auth.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Log4j2
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        log.warn("🚨 [401 Error] 인증되지 않은 사용자 접근: {}", request.getRequestURI());

        // 1. 응답 헤더 설정 (JSON)
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // 2. 응답 바디 생성
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("code", 401);
        responseMap.put("error", "Unauthorized");
        responseMap.put("message", "로그인이 필요하거나, 토큰이 만료되었습니다.");

        // 3. JSON 변환 후 전송
        response.getWriter().write(objectMapper.writeValueAsString(responseMap));
    }
}