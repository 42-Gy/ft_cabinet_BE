package com.gyeongsan.cabinet.adapter.in.web.admin;

import com.gyeongsan.cabinet.adapter.in.web.admin.dto.AdminActionLogDetailResponse;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.AdminActionLogSummaryResponse;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.UndoRequest;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.UndoResponse;
import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import com.gyeongsan.cabinet.common.ApiResponse;
import com.gyeongsan.cabinet.domain.admin.model.AdminActor;
import com.gyeongsan.cabinet.domain.admin.port.in.AdminActionLogQueryUseCase;
import com.gyeongsan.cabinet.domain.admin.port.in.AdminUndoUseCase;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 작업 감사 기록. /v4/admin/** 이므로 ADMIN 권한만 접근할 수 있다. */
@RestController
@RequestMapping("/v4/admin/action-logs")
@RequiredArgsConstructor
@RateLimiter(name = "userApi")
public class AdminActionLogController {

    private final AdminActionLogQueryUseCase adminActionLogQueryUseCase;
    private final AdminUndoUseCase adminUndoUseCase;

    @GetMapping
    public ApiResponse<Page<AdminActionLogSummaryResponse>> getActionLogs(Pageable pageable) {
        return ApiResponse.success(adminActionLogQueryUseCase.getActionLogs(pageable));
    }

    @GetMapping("/{batchId}")
    public ApiResponse<AdminActionLogDetailResponse> getActionLog(@PathVariable String batchId) {
        return ApiResponse.success(adminActionLogQueryUseCase.getActionLog(batchId));
    }

    /** 사물함 일괄 변경을 되돌린다. 그 사이 바뀐 것이 있으면 409 로 거부하고, 이미 되돌려졌어도 409 이다. */
    @PostMapping("/{batchId}/undo")
    public ApiResponse<UndoResponse> undo(
            @PathVariable String batchId,
            @RequestBody UndoRequest request,
            @AuthenticationPrincipal UserPrincipal admin) {
        UndoResponse result =
                adminUndoUseCase.undoBulkStatusUpdate(
                        batchId, request, new AdminActor(admin.getUserId(), admin.getName()));
        return ApiResponse.success(result, "되돌리기 완료");
    }
}
