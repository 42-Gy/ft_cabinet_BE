package com.gyeongsan.cabinet.admin.dto;

import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;
import com.gyeongsan.cabinet.cabinet.domain.LentType;

public record CabinetStatusRequest(
        CabinetStatus status,
        LentType lentType,
        String statusNote
) {}