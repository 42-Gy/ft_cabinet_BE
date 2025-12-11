package com.gyeongsan.cabinet.global.exception;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Log4j2
public class GlobalExceptionHandler {

    // 1. 우리가 의도적으로 발생시킨 에러 (IllegalArgumentException)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("⚠️ 잘못된 요청 감지: {}", e.getMessage());
        // 400 Bad Request 리턴
        return ResponseEntity.badRequest().body("❌ 에러: " + e.getMessage());
    }

    // 2. 예상치 못한 서버 에러 (NullPointer 등)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        log.error("🔥 서버 내부 오류 발생: ", e);
        // 500 Internal Server Error 리턴
        return ResponseEntity.internalServerError().body("🔥 서버 오류가 발생했습니다. 관리자에게 문의하세요.");
    }
}