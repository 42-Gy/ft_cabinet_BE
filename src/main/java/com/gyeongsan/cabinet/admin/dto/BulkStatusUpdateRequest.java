package com.gyeongsan.cabinet.admin.dto;

import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;

import java.util.List;

public record BulkStatusUpdateRequest(
                List<Long> cabinetIds,
                CabinetStatus status,
                String statusNote) {
}
