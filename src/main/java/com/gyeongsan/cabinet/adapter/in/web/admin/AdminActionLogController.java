package com.gyeongsan.cabinet.adapter.in.web.admin;

import com.gyeongsan.cabinet.adapter.in.web.admin.dto.AdminActionLogDetailResponse;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.AdminActionLogSummaryResponse;
import com.gyeongsan.cabinet.common.ApiResponse;
import com.gyeongsan.cabinet.domain.admin.port.in.AdminActionLogQueryUseCase;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 작업 감사 기록. /v4/admin/** 이므로 ADMIN 권한만 접근할 수 있다. */
@RestController
@RequestMapping("/v4/admin/action-logs")
@RequiredArgsConstructor
@RateLimiter(name = "userApi")
public class AdminActionLogController {

    private final AdminActionLogQueryUseCase adminActionLogQueryUseCase;

    @GetMapping
    public ApiResponse<Page<AdminActionLogSummaryResponse>> getActionLogs(Pageable pageable) {
        return ApiResponse.success(adminActionLogQueryUseCase.getActionLogs(pageable));
    }

    @GetMapping("/{batchId}")
    public ApiResponse<AdminActionLogDetailResponse> getActionLog(@PathVariable String batchId) {
        return ApiResponse.success(adminActionLogQueryUseCase.getActionLog(batchId));
    }
}
