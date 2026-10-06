package com.gyeongsan.cabinet.adapter.in.web.lent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import com.gyeongsan.cabinet.domain.lent.model.LentReturnResult;
import com.gyeongsan.cabinet.domain.lent.port.in.LentUseCase;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.global.exception.GlobalExceptionHandler;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LentControllerReturnResponseTest {

    private final LentUseCase lentUseCase = Mockito.mock(LentUseCase.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // 실제 앱과 같은 Boot 기본 ObjectMapper 로 직렬화해서 시각이 ISO 문자열로 나가는지 본다.
        ObjectMapper[] holder = new ObjectMapper[1];
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .run(ctx -> holder[0] = ctx.getBean(ObjectMapper.class));

        UserRepositoryPort userRepository = Mockito.mock(UserRepositoryPort.class);
        User user = User.of("intra09", "intra09@example.com", null, UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 9L);
        given(userRepository.findById(9L)).willReturn(Optional.of(user));

        mockMvc =
                MockMvcBuilders.standaloneSetup(new LentController(lentUseCase, userRepository))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                        .setMessageConverters(new MappingJackson2HttpMessageConverter(holder[0]))
                        .build();

        UserPrincipal principal = new UserPrincipal(user, Map.of());
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequestBuilder returnRequest(boolean force) {
        return multipart("/v4/lent/return")
                .file(new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[] {1}))
                .param("previousPassword", "1234")
                .param("forceReturn", String.valueOf(force));
    }

    @Test
    @DisplayName("반납 응답에 기존 message 와 함께 만료/연체 정보가 담긴다")
    void returnResponseIncludesExpiryInfo() throws Exception {
        LocalDateTime expiredAt = LocalDateTime.of(2026, 10, 20, 14, 23, 11);
        given(lentUseCase.endLent(eq(9L), eq("1234"), any(), eq(false), any()))
                .willReturn(
                        new LentReturnResult(
                                expiredAt, -2, true, 6, LocalDateTime.of(2026, 10, 22, 9, 0, 0)));

        mockMvc.perform(returnRequest(false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("✅ intra09님, 반납 성공! (AI 청결도 검사 통과 🧹)"))
                .andExpect(jsonPath("$.data.expiredAtIso").value("2026-10-20T14:23:11"))
                .andExpect(jsonPath("$.data.daysRemaining").value(-2))
                .andExpect(jsonPath("$.data.overdue").value(true))
                .andExpect(jsonPath("$.data.penaltyAppliedDays").value(6))
                .andExpect(jsonPath("$.data.returnedAt").value("2026-10-22T09:00:00"));
    }

    @Test
    @DisplayName("강제 반납 응답도 같은 필드를 가지고 message 만 다르다")
    void forceReturnResponse() throws Exception {
        given(lentUseCase.endLent(eq(9L), eq("1234"), any(), eq(true), any()))
                .willReturn(
                        new LentReturnResult(
                                LocalDateTime.of(2026, 10, 30, 12, 0, 0),
                                8,
                                false,
                                0,
                                LocalDateTime.of(2026, 10, 22, 9, 0, 0)));

        mockMvc.perform(returnRequest(true))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.message")
                                .value("✅ intra09님, 수동 반납 접수 완료. (AI 검사 실패로 승인 요청)"))
                .andExpect(jsonPath("$.data.daysRemaining").value(8))
                .andExpect(jsonPath("$.data.overdue").value(false))
                .andExpect(jsonPath("$.data.penaltyAppliedDays").value(0));
    }
}
