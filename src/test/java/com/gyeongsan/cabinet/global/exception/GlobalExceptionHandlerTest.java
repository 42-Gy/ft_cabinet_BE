package com.gyeongsan.cabinet.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.gyeongsan.cabinet.domain.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

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
}
