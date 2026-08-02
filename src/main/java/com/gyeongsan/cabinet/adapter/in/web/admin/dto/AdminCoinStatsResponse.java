package com.gyeongsan.cabinet.adapter.in.web.admin.dto;

import java.time.LocalDate;
import java.util.List;

public record AdminCoinStatsResponse(List<WeekCoinData> weeklyData) {
    public record WeekCoinData(
            String weekLabel,
            LocalDate startDate,
            LocalDate endDate,
            long issuedAmount,
            long usedAmount) {}
}
