package com.gyeongsan.cabinet.admin.dto;

import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;
import com.gyeongsan.cabinet.cabinet.domain.LentType;

import java.util.List;

public record BulkStatusUpdateRequest(
                List<Long> cabinetIds,
                CabinetStatus status,
                LentType lentType,
                String statusNote) {
}
