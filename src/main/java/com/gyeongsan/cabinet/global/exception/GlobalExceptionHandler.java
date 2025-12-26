package com.gyeongsan.cabinet.global.exception;

import com.gyeongsan.cabinet.common.ApiResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Log4j2
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ApiResponse<?> handleServiceException(ServiceException e) {
        ErrorCode ec = e.getErrorCode();
        log.warn("⚠️ 비즈니스 로직 예외 발생: [{}] {}", ec.getCode(), ec.getMessage());

        return ApiResponse.fail(ec.getStatus(), ec.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<String> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("⚠️ 잘못된 요청 감지: {}", e.getMessage());
        return ApiResponse.fail(HttpStatus.BAD_REQUEST, "❌ 에러: " + e.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResponse<String> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("⚠️ 지원하지 않는 메서드 요청: {}", e.getMessage());
        return ApiResponse.fail(HttpStatus.METHOD_NOT_ALLOWED, "❌ 지원하지 않는 요청 방식입니다. (GET/POST 등 메서드를 확인하세요)");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<String> handleException(Exception e) {
        log.error("🔥 서버 내부 오류 발생: ", e);
        return ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR, "🔥 서버 오류가 발생했습니다. 관리자에게 문의하세요.");
    }
}