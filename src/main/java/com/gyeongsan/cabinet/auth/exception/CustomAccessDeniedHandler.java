package com.gyeongsan.cabinet.auth.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Log4j2
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        log.warn("⛔ [403 Error] 권한 부족 접근: {}", request.getRequestURI());

        // 1. 응답 헤더 설정
        response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // 2. 응답 바디 생성
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("code", 403);
        responseMap.put("error", "Forbidden");
        responseMap.put("message", "접근 권한이 없습니다. (관리자 전용 등)");

        // 3. 전송
        response.getWriter().write(objectMapper.writeValueAsString(responseMap));
    }
}