package com.gyeongsan.cabinet.adapter.in.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.BulkStatusRejection;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.BulkStatusUpdateRequest;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.BulkStatusUpdateResponse;
import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import com.gyeongsan.cabinet.domain.admin.port.in.AdminAlarmUseCase;
import com.gyeongsan.cabinet.domain.admin.port.in.AdminBannedUserUseCase;
import com.gyeongsan.cabinet.domain.admin.port.in.AdminCabinetUseCase;
import com.gyeongsan.cabinet.domain.admin.port.in.AdminDashboardUseCase;
import com.gyeongsan.cabinet.domain.admin.port.in.AdminItemCoinUseCase;
import com.gyeongsan.cabinet.domain.admin.port.in.AdminUserUseCase;
import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.global.exception.BulkStatusUpdateRejectedException;
import com.gyeongsan.cabinet.global.exception.GlobalExceptionHandler;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminControllerBulkStatusTest {

    private static final String URL = "/v4/admin/cabinets/bundle/status";

    private final AdminCabinetUseCase adminCabinetUseCase = Mockito.mock(AdminCabinetUseCase.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminController controller =
                new AdminController(
                        Mockito.mock(AdminDashboardUseCase.class),
                        Mockito.mock(AdminUserUseCase.class),
                        adminCabinetUseCase,
                        Mockito.mock(AdminItemCoinUseCase.class),
                        Mockito.mock(AdminBannedUserUseCase.class),
                        Mockito.mock(AdminAlarmUseCase.class));
        mockMvc =
                MockMvcBuilders.standaloneSetup(controller)
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                        .build();

        User admin = User.of("admin01", "admin01@example.com", null, UserRole.ADMIN);
        ReflectionTestUtils.setField(admin, "id", 1L);
        UserPrincipal principal = new UserPrincipal(admin, Map.of());
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
    @DisplayName("endActiveLents 를 생략하면 false 로 전달되고, 호출한 관리자 이름이 서비스로 넘어간다")
    void missingEndActiveLents_defaultsToFalse_andPassesActor() throws Exception {
        given(adminCabinetUseCase.bulkUpdateCabinetStatus(any(), eq("admin01")))
                .willReturn(new BulkStatusUpdateResponse("batch-1", List.of(), List.of()));

        mockMvc.perform(
                        patch(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"cabinetIds\":[1,2],\"status\":\"AVAILABLE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("사물함 상태 일괄 변경 완료"))
                .andExpect(jsonPath("$.data.batchId").value("batch-1"));

        ArgumentCaptor<BulkStatusUpdateRequest> captor =
                ArgumentCaptor.forClass(BulkStatusUpdateRequest.class);
        verify(adminCabinetUseCase).bulkUpdateCabinetStatus(captor.capture(), eq("admin01"));
        assertThat(captor.getValue().endActiveLents()).isFalse();
        assertThat(captor.getValue().cabinetIds()).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("endActiveLents=true 는 그대로 전달된다")
    void explicitEndActiveLents_isPassedThrough() throws Exception {
        given(adminCabinetUseCase.bulkUpdateCabinetStatus(any(), eq("admin01")))
                .willReturn(new BulkStatusUpdateResponse("batch-2", List.of(), List.of()));

        mockMvc.perform(
                        patch(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"cabinetIds\":[1],\"status\":\"AVAILABLE\","
                                                + "\"endActiveLents\":true}"))
                .andExpect(status().isOk());

        ArgumentCaptor<BulkStatusUpdateRequest> captor =
                ArgumentCaptor.forClass(BulkStatusUpdateRequest.class);
        verify(adminCabinetUseCase).bulkUpdateCabinetStatus(captor.capture(), eq("admin01"));
        assertThat(captor.getValue().endActiveLents()).isTrue();
        assertThat(captor.getValue().status()).isEqualTo(CabinetStatus.AVAILABLE);
    }

    @Test
    @DisplayName("대여 중인 사물함 때문에 거부되면 409 와 함께 거부된 사물함 목록을 내려준다")
    void occupiedRejection_returns409WithDetails() throws Exception {
        BulkStatusRejection rejection =
                new BulkStatusRejection(
                        List.of(),
                        List.of(new BulkStatusRejection.OccupiedCabinet(1L, 101, 7L, "intra01")));
        given(adminCabinetUseCase.bulkUpdateCabinetStatus(any(), eq("admin01")))
                .willThrow(new BulkStatusUpdateRejectedException(rejection));

        mockMvc.perform(
                        patch(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"cabinetIds\":[1],\"status\":\"AVAILABLE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.data.occupiedCabinets[0].visibleNum").value(101))
                .andExpect(jsonPath("$.data.occupiedCabinets[0].userName").value("intra01"))
                .andExpect(jsonPath("$.data.missingCabinetIds").isEmpty());
    }

    @Test
    @DisplayName("존재하지 않는 ID 때문에 거부되면 404 와 함께 해당 ID 목록을 내려준다")
    void missingRejection_returns404WithMissingIds() throws Exception {
        given(adminCabinetUseCase.bulkUpdateCabinetStatus(any(), eq("admin01")))
                .willThrow(
                        new BulkStatusUpdateRejectedException(
                                new BulkStatusRejection(List.of(99L), List.of())));

        mockMvc.perform(
                        patch(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"cabinetIds\":[99],\"status\":\"AVAILABLE\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data.missingCabinetIds[0]").value(99));
    }

    @Test
    @DisplayName("입력 검증 실패는 기존처럼 400 으로 응답한다")
    void invalidInput_returns400() throws Exception {
        given(adminCabinetUseCase.bulkUpdateCabinetStatus(any(), eq("admin01")))
                .willThrow(new IllegalArgumentException("사물함 ID 목록이 비어있습니다."));

        mockMvc.perform(
                        patch(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"cabinetIds\":[],\"status\":\"AVAILABLE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("JSON 직렬화 확인용: 객체 매퍼로 읽어도 endActiveLents 기본값은 false 다")
    void requestRecord_defaultsEndActiveLentsToFalse() throws Exception {
        BulkStatusUpdateRequest parsed =
                new ObjectMapper()
                        .readValue(
                                "{\"cabinetIds\":[1],\"status\":\"BROKEN\"}",
                                BulkStatusUpdateRequest.class);

        assertThat(parsed.endActiveLents()).isFalse();
    }
}
