package com.gyeongsan.cabinet.global.exception;

import com.gyeongsan.cabinet.common.ApiResponse;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
        ApiResponse<String> response =
                ApiResponse.fail(HttpStatus.BAD_REQUEST, "❌ 에러: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<String>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException e) {
        log.warn("⚠️ 지원하지 않는 메서드 요청: {}", e.getMessage());
        ApiResponse<String> response =
                ApiResponse.fail(
                        HttpStatus.METHOD_NOT_ALLOWED,
                        "❌ 지원하지 않는 요청 방식입니다. (GET/POST 등 메서드를 확인하세요)");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<String>> handleMissingParameter(
            MissingServletRequestParameterException e) {
        log.warn("⚠️ 필수 파라미터 누락: {}", e.getParameterName());
        return badRequest("❌ 필수 파라미터가 누락되었습니다: " + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<String>> handleTypeMismatch(
            MethodArgumentTypeMismatchException e) {
        log.warn("⚠️ 파라미터 형식 오류: {}={}", e.getName(), e.getValue());
        return badRequest("❌ 파라미터 형식이 올바르지 않습니다: " + e.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<String>> handleNotReadable(
            HttpMessageNotReadableException e) {
        log.warn("⚠️ 요청 본문 해석 실패: {}", e.getMessage());
        return badRequest("❌ 요청 본문 형식이 올바르지 않습니다.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<String>> handleMaxUploadSize(
            MaxUploadSizeExceededException e) {
        log.warn("⚠️ 업로드 파일 크기 초과: {}", e.getMessage());
        ApiResponse<String> response =
                ApiResponse.fail(HttpStatus.PAYLOAD_TOO_LARGE, "❌ 업로드 가능한 파일 크기를 초과했습니다.");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("⚠️ 존재하지 않는 경로 요청: {}", e.getResourcePath());
        ApiResponse<String> response =
                ApiResponse.fail(HttpStatus.NOT_FOUND, "❌ 요청한 경로를 찾을 수 없습니다.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ApiResponse<String>> handleRateLimitException(RequestNotPermitted e) {
        log.warn("🛑 요청 제한 초과 (RateLimit): {}", e.getMessage());
        ApiResponse<String> response =
                ApiResponse.fail(
                        HttpStatus.TOO_MANY_REQUESTS, "너무 많은 요청을 보내셨습니다. 잠시 후 다시 시도해주세요. 🛑");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<?>> handleOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException e) {
        return handleServiceException(new ServiceException(ErrorCode.CONCURRENT_UPDATE));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleDataIntegrityViolationException(
            DataIntegrityViolationException e) {
        log.warn("⚠️ 데이터 무결성 예외 발생: {}", e.getMessage());
        ApiResponse<String> response =
                ApiResponse.fail(HttpStatus.CONFLICT, "이미 연동 처리 중이거나 사용 중인 소셜 계정입니다.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(Exception e) {
        log.error("🔥 서버 내부 오류 발생: ", e);
        ApiResponse<String> response =
                ApiResponse.fail(
                        HttpStatus.INTERNAL_SERVER_ERROR, "🔥 서버 오류가 발생했습니다. 관리자에게 문의하세요.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private ResponseEntity<ApiResponse<String>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(HttpStatus.BAD_REQUEST, message));
    }
}
