package com.gyeongsan.cabinet.adapter.in.web.admin.dto;

import java.util.List;

public record AdminFloorStatsResponse(List<FloorStat> floors) {
    public static record FloorStat(
            Integer floor,
            Long total,
            Long used,
            Long available,
            Long overdue,
            Long broken,
            Long pending,
            Long disabled) {}
}
