package com.gyeongsan.cabinet.domain.admin.service;

import com.gyeongsan.cabinet.adapter.in.web.admin.dto.AdminActionLogDetailResponse;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.AdminActionLogSummaryResponse;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionLog;
import com.gyeongsan.cabinet.domain.admin.port.in.AdminActionLogQueryUseCase;
import com.gyeongsan.cabinet.domain.admin.port.out.AdminActionLogPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminActionLogQueryService implements AdminActionLogQueryUseCase {

    static final int MAX_PAGE_SIZE = 100;

    private final AdminActionLogPort adminActionLogPort;

    @Override
    public Page<AdminActionLogSummaryResponse> getActionLogs(Pageable pageable) {
        // 정렬은 어댑터가 발생 시각 내림차순으로 고정하므로 요청의 정렬 값은 쓰지 않고, 크기만 제한한다.
        Pageable limited =
                PageRequest.of(
                        Math.max(pageable.getPageNumber(), 0),
                        Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE));
        return adminActionLogPort.findSummaries(limited).map(AdminActionLogSummaryResponse::from);
    }

    @Override
    public AdminActionLogDetailResponse getActionLog(String batchId) {
        AdminActionLog log =
                adminActionLogPort
                        .findByBatchId(batchId)
                        .orElseThrow(
                                () -> new ServiceException(ErrorCode.ADMIN_ACTION_LOG_NOT_FOUND));
        String undoneBy = adminActionLogPort.findUndoBatchIdOf(batchId).orElse(null);
        return AdminActionLogDetailResponse.of(log, undoneBy);
    }
}
