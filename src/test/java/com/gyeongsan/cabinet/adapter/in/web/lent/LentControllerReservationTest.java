package com.gyeongsan.cabinet.adapter.in.web.lent;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import com.gyeongsan.cabinet.domain.lent.port.in.LentUseCase;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.GlobalExceptionHandler;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LentControllerReservationTest {

    private final LentUseCase lentUseCase = Mockito.mock(LentUseCase.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new LentController(
                                        lentUseCase, Mockito.mock(UserRepositoryPort.class)))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                        .build();

        User user = User.of("intra09", "intra09@example.com", null, UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 9L);
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

    @Test
    @DisplayName("DELETE /v4/lent/reservation 은 내 예약을 취소하고 번호를 알려 준다")
    void cancelReservation() throws Exception {
        given(lentUseCase.cancelReservation(9L)).willReturn(201);

        mockMvc.perform(delete("/v4/lent/reservation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("✅ 201번 사물함 예약이 취소되었습니다."));
    }

    @Test
    @DisplayName("취소할 예약이 없으면 404 이다")
    void cancelReservation_none() throws Exception {
        given(lentUseCase.cancelReservation(9L))
                .willThrow(new ServiceException(ErrorCode.RESERVATION_NOT_FOUND));

        mockMvc.perform(delete("/v4/lent/reservation")).andExpect(status().isNotFound());
    }
}
