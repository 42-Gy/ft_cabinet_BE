package com.gyeongsan.cabinet.global.exception;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException; // 👈 추가
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Log4j2
public class GlobalExceptionHandler {

    // 1. 우리가 의도적으로 발생시킨 에러 (IllegalArgumentException)
    // 예: "코인이 부족합니다", "유저가 없습니다" 등
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("⚠️ 잘못된 요청 감지: {}", e.getMessage());
        // 400 Bad Request 리턴
        return ResponseEntity.badRequest().body("❌ 에러: " + e.getMessage());
    }

    // 2. [추가] 지원하지 않는 HTTP 메서드 요청 (예: POST인데 GET으로 요청 시)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<String> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("⚠️ 지원하지 않는 메서드 요청: {}", e.getMessage());
        // 405 Method Not Allowed 리턴
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("❌ 지원하지 않는 요청 방식입니다. (GET/POST 등 메서드를 확인하세요)");
    }

    // 3. 예상치 못한 서버 에러 (NullPointer, DB Connection Fail 등)
    // 가장 마지막에 모든 에러를 잡아냅니다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        log.error("🔥 서버 내부 오류 발생: ", e);
        // 500 Internal Server Error 리턴
        return ResponseEntity.internalServerError().body("🔥 서버 오류가 발생했습니다. 관리자에게 문의하세요.");
    }
}