package com.gyeongsan.cabinet.domain.admin.port.in;

import com.gyeongsan.cabinet.adapter.in.web.admin.dto.*;
import java.time.LocalDate;
import java.util.List;

public interface AdminDashboardUseCase {
    AdminDashboardResponse getDashboard();

    AdminWeeklyStatsResponse getWeeklyStats();

    AdminStoreStatsResponse getStoreStats();

    AdminFloorStatsResponse getFloorStats();

    AdminCoinStatsResponse getCoinStats();

    AdminItemUsageStatsResponse getItemUsageStats();

    List<AttendanceStatResponse> getAttendanceStats(LocalDate start, LocalDate end);
}
