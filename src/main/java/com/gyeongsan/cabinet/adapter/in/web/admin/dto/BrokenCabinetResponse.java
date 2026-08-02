package com.gyeongsan.cabinet.adapter.in.web.admin.dto;

import com.gyeongsan.cabinet.domain.cabinet.model.Cabinet;

public record BrokenCabinetResponse(
        Long cabinetId, Integer visibleNum, Integer floor, String section, String statusNote) {
    public static BrokenCabinetResponse from(Cabinet cabinet) {
        return new BrokenCabinetResponse(
                cabinet.getId(),
                cabinet.getVisibleNum(),
                cabinet.getFloor(),
                cabinet.getSection(),
                cabinet.getStatusNote());
    }
}
