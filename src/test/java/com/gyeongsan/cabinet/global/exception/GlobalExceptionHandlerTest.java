package com.gyeongsan.cabinet.global.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gyeongsan.cabinet.domain.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

    @RestController
    static class SampleController {
        @GetMapping("/sample")
        String sample(@RequestParam Integer floor) {
            return "ok";
        }

        @PostMapping("/sample")
        String sampleBody(@RequestBody SampleBody body) {
            return "ok";
        }
    }

    record SampleBody(String name) {}

    private final MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new SampleController())
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .build();

    @Test
    @DisplayName("필수 파라미터가 빠지면 400으로 응답한다")
    void missingParameter_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/sample"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("파라미터 형식이 틀리면 400으로 응답한다")
    void typeMismatch_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/sample").param("floor", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("요청 본문 JSON이 깨지면 400으로 응답한다")
    void malformedJson_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/sample").contentType(MediaType.APPLICATION_JSON).content("{bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("업로드 파일이 너무 크면 413으로 응답한다")
    void maxUploadSizeExceeded_returnsPayloadTooLarge() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var response = handler.handleMaxUploadSize(new MaxUploadSizeExceededException(10L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }

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
