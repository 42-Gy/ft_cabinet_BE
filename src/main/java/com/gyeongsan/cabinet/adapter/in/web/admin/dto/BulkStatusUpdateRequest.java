package com.gyeongsan.cabinet.adapter.in.web.admin.dto;

import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import com.gyeongsan.cabinet.domain.cabinet.model.LentType;
import java.util.List;

public record BulkStatusUpdateRequest(
        List<Long> cabinetIds, CabinetStatus status, LentType lentType, String statusNote) {}
