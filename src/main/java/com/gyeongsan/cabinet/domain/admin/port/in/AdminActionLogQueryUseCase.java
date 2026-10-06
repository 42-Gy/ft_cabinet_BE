package com.gyeongsan.cabinet.domain.admin.port.in;

import com.gyeongsan.cabinet.adapter.in.web.admin.dto.AdminActionLogDetailResponse;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.AdminActionLogSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminActionLogQueryUseCase {

    /** 최신순 목록. 한 번에 가져오는 개수는 상한(100)을 둔다. */
    Page<AdminActionLogSummaryResponse> getActionLogs(Pageable pageable);

    AdminActionLogDetailResponse getActionLog(String batchId);
}
