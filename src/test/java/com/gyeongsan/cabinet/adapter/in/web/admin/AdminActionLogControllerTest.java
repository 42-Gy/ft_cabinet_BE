package com.gyeongsan.cabinet.adapter.in.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gyeongsan.cabinet.adapter.in.web.admin.dto.AdminActionLogDetailResponse;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.AdminActionLogSummaryResponse;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.UndoRejection;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.UndoRequest;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.UndoResponse;
import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionTargetType;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionType;
import com.gyeongsan.cabinet.domain.admin.model.AdminActor;
import com.gyeongsan.cabinet.domain.admin.model.UndoConflictCode;
import com.gyeongsan.cabinet.domain.admin.port.in.AdminActionLogQueryUseCase;
import com.gyeongsan.cabinet.domain.admin.port.in.AdminUndoUseCase;
import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.GlobalExceptionHandler;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import com.gyeongsan.cabinet.global.exception.UndoRejectedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminActionLogControllerTest {

    private static final AdminActor ADMIN = new AdminActor(1L, "admin01");

    private final AdminActionLogQueryUseCase queryUseCase =
            Mockito.mock(AdminActionLogQueryUseCase.class);
    private final AdminUndoUseCase undoUseCase = Mockito.mock(AdminUndoUseCase.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new AdminActionLogController(queryUseCase, undoUseCase))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .setCustomArgumentResolvers(
                                new PageableHandlerMethodArgumentResolver(),
                                new AuthenticationPrincipalArgumentResolver())
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
    @DisplayName("목록은 페이지 형태로 내려가고 Undo 여부를 담는다")
    void list() throws Exception {
        given(queryUseCase.getActionLogs(any()))
                .willReturn(
                        new PageImpl<>(
                                List.of(
                                        new AdminActionLogSummaryResponse(
                                                "b-1",
                                                AdminActionType.CABINET_BULK_STATUS_UPDATE,
                                                1L,
                                                "admin01",
                                                "월말",
                                                LocalDateTime.of(2026, 10, 5, 12, 0),
                                                3,
                                                null,
                                                "u-1")),
                                PageRequest.of(0, 20),
                                1));

        mockMvc.perform(get("/v4/admin/action-logs?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].batchId").value("b-1"))
                .andExpect(jsonPath("$.data.content[0].itemCount").value(3))
                .andExpect(jsonPath("$.data.content[0].undoneByBatchId").value("u-1"));
    }

    @Test
    @DisplayName("상세는 항목의 변경 전/후 값을 담는다")
    void detail() throws Exception {
        given(queryUseCase.getActionLog("b-1"))
                .willReturn(
                        new AdminActionLogDetailResponse(
                                "b-1",
                                AdminActionType.CABINET_BULK_STATUS_UPDATE,
                                1L,
                                "admin01",
                                "월말",
                                LocalDateTime.of(2026, 10, 5, 12, 0),
                                null,
                                null,
                                Map.of("status", "AVAILABLE"),
                                List.of(
                                        new AdminActionLogDetailResponse.Item(
                                                AdminActionTargetType.CABINET,
                                                7L,
                                                "101",
                                                Map.of("status", "FULL"),
                                                Map.of("status", "AVAILABLE")))));

        mockMvc.perform(get("/v4/admin/action-logs/b-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].before.status").value("FULL"))
                .andExpect(jsonPath("$.data.items[0].after.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.request.status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("없는 batchId 는 404 이다")
    void detailNotFound() throws Exception {
        given(queryUseCase.getActionLog("none"))
                .willThrow(new ServiceException(ErrorCode.ADMIN_ACTION_LOG_NOT_FOUND));

        mockMvc.perform(get("/v4/admin/action-logs/none")).andExpect(status().isNotFound());
    }

    // ---------- Undo ----------

    @Test
    @DisplayName("Undo 는 batchId 와 사유, 호출한 관리자(ID, 이름)를 서비스로 넘기고 요약을 돌려준다")
    void undo_success() throws Exception {
        given(
                        undoUseCase.undoBulkStatusUpdate(
                                eq("b-1"), org.mockito.ArgumentMatchers.any(), eq(ADMIN)))
                .willReturn(
                        new UndoResponse(
                                "u-1",
                                "b-1",
                                List.of(
                                        new UndoResponse.RestoredCabinet(
                                                7L,
                                                101,
                                                CabinetStatus.AVAILABLE,
                                                CabinetStatus.FULL)),
                                List.of(
                                        new UndoResponse.ReopenedLent(
                                                10L, 7L, 101, 3L, "intra01"))));

        mockMvc.perform(
                        post("/v4/admin/action-logs/b-1/undo")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"실수로 일괄 반납함\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.undoBatchId").value("u-1"))
                .andExpect(jsonPath("$.data.restoredCabinets[0].toStatus").value("FULL"))
                .andExpect(jsonPath("$.data.reopenedLents[0].userName").value("intra01"));

        ArgumentCaptor<UndoRequest> captor = ArgumentCaptor.forClass(UndoRequest.class);
        verify(undoUseCase).undoBulkStatusUpdate(eq("b-1"), captor.capture(), eq(ADMIN));
        assertThat(captor.getValue().reason()).isEqualTo("실수로 일괄 반납함");
    }

    @Test
    @DisplayName("충돌이 있으면 409 와 함께 충돌 목록(코드, 사물함, 설명)을 내려준다")
    void undo_conflicts_returns409WithDetails() throws Exception {
        given(
                        undoUseCase.undoBulkStatusUpdate(
                                eq("b-1"), org.mockito.ArgumentMatchers.any(), eq(ADMIN)))
                .willThrow(
                        new UndoRejectedException(
                                new UndoRejection(
                                        null,
                                        List.of(
                                                new UndoRejection.Conflict(
                                                        UndoConflictCode.USER_HAS_ACTIVE_LENT,
                                                        7L,
                                                        101,
                                                        3L,
                                                        "사용자가 지금 다른 사물함(202)을 대여 중입니다.")))));

        mockMvc.perform(
                        post("/v4/admin/action-logs/b-1/undo")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"되돌림\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.conflicts[0].code").value("USER_HAS_ACTIVE_LENT"))
                .andExpect(jsonPath("$.data.conflicts[0].visibleNum").value(101))
                .andExpect(jsonPath("$.data.undoneByBatchId").isEmpty());
    }

    @Test
    @DisplayName("이미 되돌려진 작업은 409 와 함께 어떤 Undo 였는지 알려 준다")
    void undo_alreadyUndone_returns409WithUndoneBy() throws Exception {
        given(
                        undoUseCase.undoBulkStatusUpdate(
                                eq("b-1"), org.mockito.ArgumentMatchers.any(), eq(ADMIN)))
                .willThrow(
                        new UndoRejectedException(
                                new UndoRejection(
                                        "u-0",
                                        List.of(
                                                new UndoRejection.Conflict(
                                                        UndoConflictCode.ALREADY_UNDONE,
                                                        null,
                                                        null,
                                                        null,
                                                        "이미 되돌려진 작업입니다")))));

        mockMvc.perform(
                        post("/v4/admin/action-logs/b-1/undo")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"되돌림\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.undoneByBatchId").value("u-0"))
                .andExpect(jsonPath("$.data.conflicts[0].code").value("ALREADY_UNDONE"));
    }

    @Test
    @DisplayName("사유가 없으면 400, 없는 batchId 는 404 이다")
    void undo_badRequestAndNotFound() throws Exception {
        given(
                        undoUseCase.undoBulkStatusUpdate(
                                eq("b-1"), org.mockito.ArgumentMatchers.any(), eq(ADMIN)))
                .willThrow(new IllegalArgumentException("되돌리기 사유(reason)는 필수입니다."));
        given(
                        undoUseCase.undoBulkStatusUpdate(
                                eq("none"), org.mockito.ArgumentMatchers.any(), eq(ADMIN)))
                .willThrow(new ServiceException(ErrorCode.ADMIN_ACTION_LOG_NOT_FOUND));

        mockMvc.perform(
                        post("/v4/admin/action-logs/b-1/undo")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                        post("/v4/admin/action-logs/none/undo")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"되돌림\"}"))
                .andExpect(status().isNotFound());
    }
}
