package com.gyeongsan.cabinet.global.exception;

import com.gyeongsan.cabinet.common.ApiResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Log4j2
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ApiResponse<?>> handleServiceException(ServiceException e) {
        ErrorCode ec = e.getErrorCode();
        log.warn("⚠️ 비즈니스 로직 예외 발생: [{}] {}", ec.getCode(), ec.getMessage());

        ApiResponse<?> response = ApiResponse.fail(ec.getStatus(), ec.getMessage());
        return ResponseEntity.status(ec.getStatus()).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<String>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("⚠️ 잘못된 요청 감지: {}", e.getMessage());
        ApiResponse<String> response = ApiResponse.fail(HttpStatus.BAD_REQUEST, "❌ 에러: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<String>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("⚠️ 지원하지 않는 메서드 요청: {}", e.getMessage());
        ApiResponse<String> response = ApiResponse.fail(HttpStatus.METHOD_NOT_ALLOWED,
                "❌ 지원하지 않는 요청 방식입니다. (GET/POST 등 메서드를 확인하세요)");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ApiResponse<String>> handleRateLimitException(RequestNotPermitted e) {
        log.warn("🛑 요청 제한 초과 (RateLimit): {}", e.getMessage());
        ApiResponse<String> response = ApiResponse.fail(HttpStatus.TOO_MANY_REQUESTS,
                "너무 많은 요청을 보내셨습니다. 잠시 후 다시 시도해주세요. 🛑");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(Exception e) {
        log.error("🔥 서버 내부 오류 발생: ", e);
        ApiResponse<String> response = ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR,
                "🔥 서버 오류가 발생했습니다. 관리자에게 문의하세요.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}