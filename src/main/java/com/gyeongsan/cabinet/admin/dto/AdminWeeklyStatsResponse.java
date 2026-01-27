package com.gyeongsan.cabinet.admin.dto;

import java.time.LocalDate;
import java.util.List;

public record AdminWeeklyStatsResponse(
                List<WeekData> weeklyData) {
        public static record WeekData(
                        String weekLabel,
                        LocalDate startDate,
                        LocalDate endDate,
                        Long lentsStarted,
                        Long lentsEnded) {
        }
}
