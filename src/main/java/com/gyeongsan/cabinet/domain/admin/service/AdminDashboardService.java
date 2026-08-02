package com.gyeongsan.cabinet.domain.admin.service;

import com.gyeongsan.cabinet.adapter.in.web.admin.dto.*;
import com.gyeongsan.cabinet.adapter.out.persistence.cabinet.CabinetRepository;
import com.gyeongsan.cabinet.domain.admin.port.in.AdminDashboardUseCase;
import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import com.gyeongsan.cabinet.domain.cabinet.port.out.CabinetRepositoryPort;
import com.gyeongsan.cabinet.domain.coin.model.CoinLogType;
import com.gyeongsan.cabinet.domain.coin.port.out.CoinHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.item.port.out.ItemHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.domain.user.port.out.AttendanceRepositoryPort;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService implements AdminDashboardUseCase {

    private final UserRepositoryPort userRepository;
    private final CabinetRepositoryPort cabinetRepository;
    private final LentRepositoryPort lentRepository;
    private final ItemHistoryRepositoryPort itemHistoryRepository;
    private final AttendanceRepositoryPort attendanceRepository;
    private final CoinHistoryRepositoryPort coinHistoryRepository;

    @Override
    public AdminDashboardResponse getDashboard() {
        long bannedUserCount = userRepository.countByPenaltyDaysGreaterThan(0);

        return new AdminDashboardResponse(
                userRepository.count(),
                cabinetRepository.count(),
                lentRepository.countByEndedAtIsNull(),
                cabinetRepository.countByStatus(CabinetStatus.BROKEN),
                bannedUserCount);
    }

    @Override
    public AdminWeeklyStatsResponse getWeeklyStats() {
        LocalDateTime now = LocalDateTime.now();
        List<AdminWeeklyStatsResponse.WeekData> weeklyData = new ArrayList<>();

        for (int i = 3; i >= 0; i--) {
            LocalDateTime weekEnd = now.minusWeeks(i);
            LocalDateTime weekStart = weekEnd.minusWeeks(1);

            long started = lentRepository.countLentsStartedAtBetween(weekStart, weekEnd);
            long ended = lentRepository.countLentsEndedAtBetween(weekStart, weekEnd);

            String label =
                    String.format(
                            "Week %d (%s - %s)",
                            4 - i, weekStart.toLocalDate(), weekEnd.toLocalDate());

            weeklyData.add(
                    new AdminWeeklyStatsResponse.WeekData(
                            label, weekStart.toLocalDate(), weekEnd.toLocalDate(), started, ended));
        }

        return new AdminWeeklyStatsResponse(weeklyData);
    }

    @Override
    public AdminStoreStatsResponse getStoreStats() {
        long totalUserCoins = userRepository.sumCoins().orElse(0L);
        long totalUsedCoins =
                itemHistoryRepository.sumUsedItemPrice() != null
                        ? itemHistoryRepository.sumUsedItemPrice()
                        : 0L;

        List<Object[]> salesData = itemHistoryRepository.findItemSales();
        Map<String, Long> itemSales = new HashMap<>();

        for (Object[] row : salesData) {
            String itemName = (String) row[0];
            Long count = (Long) row[1];
            itemSales.put(itemName, count);
        }

        return new AdminStoreStatsResponse(totalUserCoins, totalUsedCoins, itemSales);
    }

    @Override
    public AdminFloorStatsResponse getFloorStats() {
        List<CabinetRepository.FloorStatProjection> projections =
                cabinetRepository.findFloorStatistics();

        List<AdminFloorStatsResponse.FloorStat> floors =
                projections.stream()
                        .map(
                                p ->
                                        new AdminFloorStatsResponse.FloorStat(
                                                p.getFloor(),
                                                p.getTotal(),
                                                p.getUsed(),
                                                p.getAvailable(),
                                                p.getOverdue(),
                                                p.getBroken(),
                                                p.getPending(),
                                                p.getDisabled()))
                        .collect(Collectors.toList());

        return new AdminFloorStatsResponse(floors);
    }

    @Override
    public AdminCoinStatsResponse getCoinStats() {
        LocalDateTime now = LocalDateTime.now();
        List<AdminCoinStatsResponse.WeekCoinData> weeklyData = new ArrayList<>();

        for (int i = 3; i >= 0; i--) {
            LocalDateTime weekEnd = now.minusWeeks(i);
            LocalDateTime weekStart = weekEnd.minusWeeks(1);

            List<Object[]> resultList =
                    coinHistoryRepository.sumIssuedAndUsedBetween(weekStart, weekEnd);
            Object[] result = resultList.get(0);

            long issued = result[0] != null ? ((Number) result[0]).longValue() : 0L;
            long used = result[1] != null ? ((Number) result[1]).longValue() : 0L;

            String label =
                    String.format(
                            "Week %d (%s - %s)",
                            4 - i, weekStart.toLocalDate(), weekEnd.toLocalDate());

            weeklyData.add(
                    new AdminCoinStatsResponse.WeekCoinData(
                            label, weekStart.toLocalDate(), weekEnd.toLocalDate(), issued, used));
        }

        return new AdminCoinStatsResponse(weeklyData);
    }

    @Override
    public AdminItemUsageStatsResponse getItemUsageStats() {
        List<Object[]> statsData = itemHistoryRepository.findItemUsageStats();

        List<AdminItemUsageStatsResponse.ItemUsageStat> itemStats =
                statsData.stream()
                        .map(
                                row ->
                                        new AdminItemUsageStatsResponse.ItemUsageStat(
                                                (String) row[0],
                                                row[1].toString(),
                                                ((Number) row[2]).longValue(),
                                                ((Number) row[3]).longValue()))
                        .collect(Collectors.toList());

        long attendanceCount =
                coinHistoryRepository.countByTypeAndCreatedAtBetween(
                        CoinLogType.ATTENDANCE,
                        LocalDateTime.of(2000, 1, 1, 0, 0),
                        LocalDateTime.now());

        long watermelonCount =
                coinHistoryRepository.countByTypeAndCreatedAtBetween(
                        CoinLogType.WATERMELON,
                        LocalDateTime.of(2000, 1, 1, 0, 0),
                        LocalDateTime.now());

        return new AdminItemUsageStatsResponse(itemStats, attendanceCount, watermelonCount);
    }

    @Override
    public List<AttendanceStatResponse> getAttendanceStats(LocalDate start, LocalDate end) {
        if (start == null) start = LocalDate.now().minusDays(30);
        if (end == null) end = LocalDate.now();

        List<Object[]> counts = attendanceRepository.getDailyAttendanceCounts(start, end);

        return counts.stream()
                .map(row -> new AttendanceStatResponse((LocalDate) row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }
}
