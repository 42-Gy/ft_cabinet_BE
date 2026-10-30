package com.gyeongsan.cabinet.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.gyeongsan.cabinet.domain.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

    @Test
    @DisplayName("동시 수정 충돌은 500이 아닌 409로 응답한다")
    void optimisticLockingFailure_returnsConflict() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var response =
                handler.handleOptimisticLockingFailure(
                        new ObjectOptimisticLockingFailureException(User.class, 1L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("없는 경로 요청은 500이 아닌 404로 응답한다")
    void noResourceFound_returnsNotFound() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var response =
                handler.handleNoResourceFound(
                        new NoResourceFoundException(HttpMethod.GET, "v3/api-docs"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
