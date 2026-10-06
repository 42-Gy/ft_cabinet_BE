package com.gyeongsan.cabinet.adapter.in.web.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gyeongsan.cabinet.adapter.in.web.admin.dto.AdminActionLogDetailResponse;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.AdminActionLogSummaryResponse;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionTargetType;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionType;
import com.gyeongsan.cabinet.domain.admin.port.in.AdminActionLogQueryUseCase;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.GlobalExceptionHandler;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminActionLogControllerTest {

    private final AdminActionLogQueryUseCase queryUseCase =
            Mockito.mock(AdminActionLogQueryUseCase.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(new AdminActionLogController(queryUseCase))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                        .build();
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
}
