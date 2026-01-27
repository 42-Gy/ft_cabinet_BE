package com.gyeongsan.cabinet.admin.service;

import com.gyeongsan.cabinet.admin.dto.*;
import com.gyeongsan.cabinet.alarm.SlackBotService;
import com.gyeongsan.cabinet.cabinet.domain.Cabinet;
import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;

import com.gyeongsan.cabinet.cabinet.repository.CabinetRepository;
import com.gyeongsan.cabinet.coin.domain.CoinHistory;
import com.gyeongsan.cabinet.coin.domain.CoinLogType;
import com.gyeongsan.cabinet.coin.repository.CoinHistoryRepository;
import com.gyeongsan.cabinet.item.domain.ItemHistory;
import com.gyeongsan.cabinet.item.repository.ItemHistoryRepository;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import com.gyeongsan.cabinet.item.domain.Item;
import com.gyeongsan.cabinet.item.repository.ItemRepository;
import com.gyeongsan.cabinet.lent.domain.LentHistory;
import com.gyeongsan.cabinet.lent.repository.LentRepository;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.domain.UserRole;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import com.gyeongsan.cabinet.user.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class AdminService {

        private final UserRepository userRepository;
        private final CabinetRepository cabinetRepository;
        private final LentRepository lentRepository;
        private final ItemRepository itemRepository;
        private final ItemHistoryRepository itemHistoryRepository;
        private final SlackBotService slackBotService;
        private final AttendanceRepository attendanceRepository;
        private final CoinHistoryRepository coinHistoryRepository;

        @Transactional(readOnly = true)
        public AdminDashboardResponse getDashboard() {
                long bannedUserCount = userRepository.countByPenaltyDaysGreaterThan(0);

                return new AdminDashboardResponse(
                                userRepository.count(),
                                cabinetRepository.count(),
                                lentRepository.countByEndedAtIsNull(),
                                cabinetRepository.countByStatus(CabinetStatus.BROKEN),
                                bannedUserCount);
        }

        @Transactional(readOnly = true)
        public AdminUserDetailResponse getUserDetail(String name) {
                User user = userRepository.findByName(name)
                                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

                Integer currentCabinetNum = lentRepository.findByUserIdAndEndedAtIsNull(user.getId())
                                .map(lent -> lent.getCabinet().getVisibleNum())
                                .orElse(null);

                Map<String, Integer> itemCounts = itemHistoryRepository.findAllByUserIdAndUsedAtIsNull(user.getId())
                                .stream()
                                .collect(Collectors.groupingBy(
                                                itemHistory -> itemHistory.getItem().getType().name(),
                                                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

                return AdminUserDetailResponse.of(user, currentCabinetNum, itemCounts);
        }

        @Transactional(readOnly = true)
        public Page<CabinetHistoryResponse> getCabinetHistory(Integer visibleNum, Pageable pageable) {
                cabinetRepository.findByVisibleNumWithLock(visibleNum)
                                .orElseThrow(() -> new ServiceException(ErrorCode.CABINET_NOT_FOUND));

                return lentRepository.findHistoryByCabinet(visibleNum, pageable)
                                .map(CabinetHistoryResponse::from);
        }

        public void updateUserLogtime(String username, Integer newLogtime) {
                User user = userRepository.findByName(username)
                                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

                user.updateMonthlyLogtime(newLogtime);
                log.info("[Admin] 유저({}) 로그타임 수정 완료: {}분", user.getName(), newLogtime);
        }

        public void provideCoin(String username, CoinProvideRequest request) {
                User user = userRepository.findByName(username)
                                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

                user.addCoin(request.amount());

                CoinHistory adminGrant = CoinHistory.of(user, request.amount(), CoinLogType.ADMIN_GRANT);
                coinHistoryRepository.save(adminGrant);

                log.info(
                                "[Admin] 유저({})에게 코인 {}개 지급. 사유: {}",
                                user.getName(),
                                request.amount(),
                                request.reason());
        }

        public void updateCabinetStatus(Integer visibleNum, CabinetStatusRequest request) {
                Cabinet cabinet = cabinetRepository.findByVisibleNumWithLock(visibleNum)
                                .orElseThrow(() -> new ServiceException(ErrorCode.CABINET_NOT_FOUND));

                cabinet.updateStatus(
                                request.status(),
                                request.lentType(),
                                request.statusNote());

                log.info(
                                "[Admin] 사물함({}) 상태 변경: {}, {}, {}",
                                visibleNum,
                                request.status(),
                                request.lentType(),
                                request.statusNote());
        }

        public void forceReturn(Integer visibleNum) {
                Cabinet cabinet = cabinetRepository.findByVisibleNumWithLock(visibleNum)
                                .orElseThrow(() -> new ServiceException(ErrorCode.CABINET_NOT_FOUND));

                LentHistory activeLent = lentRepository.findByCabinetIdAndEndedAtIsNull(cabinet.getId())
                                .orElse(null);

                if (activeLent != null) {
                        activeLent.endLent(LocalDateTime.now(), "관리자 강제 반납");
                }

                cabinet.updateStatus(
                                CabinetStatus.PENDING,
                                cabinet.getLentType(),
                                "강제 반납: 물품 수거 및 청소 필요");

                log.info("[Admin] 사물함({}) 강제 반납 완료 -> 상태: PENDING", visibleNum);
        }

        @Transactional(readOnly = true)
        public List<CabinetPendingResponseDto> getPendingCabinets() {
                List<LentHistory> pendingLentHistories = lentRepository.findAllLatestLentForPendingCabinets();

                return pendingLentHistories.stream()
                                .map(lh -> new CabinetPendingResponseDto(
                                                lh.getCabinet().getVisibleNum(),
                                                lh.getCabinet().getStatusNote(),
                                                lh.getCabinet().getLentType(),
                                                lh.getPhotoUrl(),
                                                lh.getUser().getName()))
                                .collect(Collectors.toList());
        }

        public void approveManualReturn(Integer visibleNum) {
                Cabinet cabinet = cabinetRepository.findByVisibleNumWithLock(visibleNum)
                                .orElseThrow(() -> new ServiceException(ErrorCode.CABINET_NOT_FOUND));

                if (cabinet.getStatus() != CabinetStatus.PENDING) {
                        throw new ServiceException(ErrorCode.INVALID_CABINET_STATUS);
                }

                cabinet.updateStatus(
                                CabinetStatus.AVAILABLE,
                                cabinet.getLentType(),
                                null);

                log.info("[Admin] 사물함({}) 수동 반납 승인 완료 -> AVAILABLE", visibleNum);
        }

        public void updateItemPrice(String itemName, Long newPrice) {
                Item item = itemRepository.findByName(itemName)
                                .orElseThrow(() -> new ServiceException(ErrorCode.ITEM_NOT_FOUND));

                if (newPrice < 0) {
                        throw new IllegalArgumentException("가격은 0원 이상이어야 합니다.");
                }

                Long oldPrice = item.getPrice();
                item.updatePrice(newPrice);

                log.info(
                                "[Admin] 아이템({}) 가격 변경 완료: {} -> {}",
                                item.getName(),
                                oldPrice,
                                newPrice);
        }

        @Transactional(readOnly = true)
        public List<OverdueUserResponse> getOverdueUsers() {
                return lentRepository.findAllOverdueLentHistories(LocalDateTime.now())
                                .stream()
                                .map(OverdueUserResponse::from)
                                .collect(Collectors.toList());
        }

        public void givePenalty(String username, PenaltyRequest request) {
                User user = userRepository.findByName(username)
                                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

                user.updatePenaltyDays(request.penaltyDays());
                log.info("[Admin] 유저({})에게 패널티 {}일 부여. 사유: {}", username, request.penaltyDays(), request.reason());
        }

        public void deletePenalty(String username) {
                User user = userRepository.findByName(username)
                                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

                user.clearPenalty();
                log.info("[Admin] 유저({}) 패널티 해제 완료", username);
        }

        public void grantItem(String username, ItemGrantRequest request) {
                User user = userRepository.findByName(username)
                                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

                Item item = itemRepository.findByName(request.itemName())
                                .orElseThrow(() -> new ServiceException(ErrorCode.ITEM_NOT_FOUND));

                ItemHistory itemHistory = new ItemHistory(LocalDateTime.now(), null, user, item);
                itemHistoryRepository.save(itemHistory);

                log.info("[Admin] 유저({})에게 아이템({}) 지급. 사유: {}", username, request.itemName(), request.reason());
        }

        public void sendEmergencyNotice(String message) {
                List<LentHistory> activeLents = lentRepository.findAllActiveLents();

                int successCount = 0;
                for (LentHistory lent : activeLents) {
                        try {
                                String intraId = lent.getUser().getName();
                                slackBotService.sendDm(intraId, "[Cabi 긴급공지] " + message);
                                successCount++;
                        } catch (Exception e) {
                                log.error("Emergency DM send failed for user: {}", lent.getUser().getName());
                        }
                }
                log.info("[Admin] 긴급 공지 전송 완료. 대상: {}명, 성공: {}명", activeLents.size(), successCount);
        }

        @Transactional(readOnly = true)
        public AdminWeeklyStatsResponse getWeeklyStats() {
                LocalDateTime now = LocalDateTime.now();
                List<AdminWeeklyStatsResponse.WeekData> weeklyData = new ArrayList<>();

                for (int i = 3; i >= 0; i--) {
                        LocalDateTime weekEnd = now.minusWeeks(i);
                        LocalDateTime weekStart = weekEnd.minusWeeks(1);

                        long started = lentRepository.countLentsStartedAtBetween(weekStart, weekEnd);
                        long ended = lentRepository.countLentsEndedAtBetween(weekStart, weekEnd);

                        String label = String.format("Week %d (%s - %s)",
                                        4 - i,
                                        weekStart.toLocalDate(),
                                        weekEnd.toLocalDate());

                        weeklyData.add(new AdminWeeklyStatsResponse.WeekData(
                                        label,
                                        weekStart.toLocalDate(),
                                        weekEnd.toLocalDate(),
                                        started,
                                        ended));
                }

                return new AdminWeeklyStatsResponse(weeklyData);
        }

        @Transactional(readOnly = true)
        public CabinetDetailResponse getCabinetDetail(Integer visibleNum) {
                Cabinet cabinet = cabinetRepository.findByVisibleNumWithLock(visibleNum)
                                .orElseThrow(() -> new ServiceException(ErrorCode.CABINET_NOT_FOUND));

                LentHistory activeLent = lentRepository.findByCabinetIdAndEndedAtIsNull(cabinet.getId())
                                .orElse(null);

                return CabinetDetailResponse.of(cabinet, activeLent);
        }

        @Transactional(readOnly = true)
        public AdminStoreStatsResponse getStoreStats() {
                long totalUserCoins = userRepository.sumCoins().orElse(0L);
                long totalUsedCoins = itemHistoryRepository.sumUsedItemPrice() != null
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

        public List<AttendanceStatResponse> getAttendanceStats(LocalDate start, LocalDate end) {

                if (start == null)
                        start = LocalDate.now().minusDays(30);
                if (end == null)
                        end = LocalDate.now();

                List<Object[]> counts = attendanceRepository.getDailyAttendanceCounts(start, end);

                return counts.stream()
                                .map(row -> new AttendanceStatResponse((LocalDate) row[0], (Long) row[1]))
                                .collect(Collectors.toList());
        }

        public void promoteUserToAdmin(String username) {
                User user = userRepository.findByName(username)
                                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));
                user.updateRole(UserRole.ADMIN);
                log.info("[Admin] 유저({}) 권한 변경: USER -> ADMIN", username);
        }

        public void demoteUserToUser(String username) {
                User user = userRepository.findByName(username)
                                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));
                user.updateRole(UserRole.USER);
                log.info("[Admin] 유저({}) 권한 변경: ADMIN -> USER", username);
        }

        public void revokeUserItem(String username, ItemRevokeRequest request) {
                User user = userRepository.findByName(username)
                                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

                Item item = itemRepository.findByName(request.itemName())
                                .orElseThrow(() -> new ServiceException(ErrorCode.ITEM_NOT_FOUND));

                List<ItemHistory> unusedItems = itemHistoryRepository.findUnusedItems(user.getId(), item.getType());

                if (request.amount() != null) {
                        unusedItems = unusedItems.stream()
                                        .limit(request.amount())
                                        .collect(Collectors.toList());
                }

                itemHistoryRepository.deleteAll(unusedItems);
                log.info("[Admin] 유저({})의 아이템({}) {}개 회수 완료", username, request.itemName(), unusedItems.size());
        }

        public void revokeUserCoin(String username, CoinRevokeRequest request) {
                User user = userRepository.findByName(username)
                                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

                long amountToRevoke = request.amount();
                if (amountToRevoke <= 0) {
                        throw new IllegalArgumentException("회수할 코인은 0보다 커야 합니다.");
                }

                user.useCoin(amountToRevoke);

                CoinHistory adminRevoke = CoinHistory.of(user, -amountToRevoke, CoinLogType.ADMIN_REVOKE);
                coinHistoryRepository.save(adminRevoke);

                log.info("[Admin] 유저({}) 코인 회수: {}개. 사유: {}", username, amountToRevoke, request.reason());
        }

        @Transactional(readOnly = true)
        public AdminFloorStatsResponse getFloorStats() {
                List<CabinetRepository.FloorStatProjection> projections = cabinetRepository.findFloorStatistics();

                List<AdminFloorStatsResponse.FloorStat> floors = projections.stream()
                                .map(p -> new AdminFloorStatsResponse.FloorStat(
                                                p.getFloor(),
                                                p.getTotal(),
                                                p.getUsed(),
                                                p.getAvailable(),
                                                p.getOverdue(),
                                                p.getBroken(),
                                                p.getPending()))
                                .collect(Collectors.toList());

                return new AdminFloorStatsResponse(floors);
        }

        @Transactional(readOnly = true)
        public List<PenaltyUserResponse> getPenaltyUsers() {
                return userRepository.findAllPenaltyUsers()
                                .stream()
                                .map(PenaltyUserResponse::from)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<BrokenCabinetResponse> getBrokenCabinets() {
                return cabinetRepository.findAllByStatus(CabinetStatus.BROKEN)
                                .stream()
                                .map(BrokenCabinetResponse::from)
                                .collect(Collectors.toList());
        }

        public void bulkUpdateCabinetStatus(BulkStatusUpdateRequest request) {
                List<Long> cabinetIds = request.cabinetIds();
                CabinetStatus targetStatus = request.status();
                String statusNote = request.statusNote();

                if (cabinetIds == null || cabinetIds.isEmpty()) {
                        throw new IllegalArgumentException("사물함 ID 목록이 비어있습니다.");
                }

                if (targetStatus == CabinetStatus.BROKEN && (statusNote == null || statusNote.isBlank())) {
                        throw new IllegalArgumentException("고장 상태로 변경 시 사유(statusNote)는 필수입니다.");
                }

                List<Cabinet> cabinets = cabinetRepository.findAllById(cabinetIds);

                if (cabinets.size() != cabinetIds.size()) {
                        log.warn("[Admin] 일부 사물함을 찾을 수 없습니다. 요청: {}, 발견: {}", cabinetIds.size(), cabinets.size());
                }

                for (Cabinet cabinet : cabinets) {
                        cabinet.updateStatus(targetStatus);
                        if (statusNote != null && !statusNote.isBlank()) {
                                cabinet.updateStatusNote(statusNote);
                        }
                }

                log.info("[Admin] 사물함 상태 일괄 변경 완료: {} -> {} (사유: {}, 대상: {}개)",
                                cabinetIds.size(), targetStatus, statusNote, cabinets.size());
        }

        @Transactional(readOnly = true)
        public AdminCoinStatsResponse getCoinStats() {
                LocalDateTime now = LocalDateTime.now();
                List<AdminCoinStatsResponse.WeekCoinData> weeklyData = new ArrayList<>();

                for (int i = 3; i >= 0; i--) {
                        LocalDateTime weekEnd = now.minusWeeks(i);
                        LocalDateTime weekStart = weekEnd.minusWeeks(1);

                        Object[] result = coinHistoryRepository.sumIssuedAndUsedBetween(weekStart, weekEnd);

                        long issued = result[0] != null ? ((Number) result[0]).longValue() : 0L;
                        long used = result[1] != null ? ((Number) result[1]).longValue() : 0L;

                        String label = String.format("Week %d (%s - %s)",
                                        4 - i,
                                        weekStart.toLocalDate(),
                                        weekEnd.toLocalDate());

                        weeklyData.add(new AdminCoinStatsResponse.WeekCoinData(
                                        label,
                                        weekStart.toLocalDate(),
                                        weekEnd.toLocalDate(),
                                        issued,
                                        used));
                }

                return new AdminCoinStatsResponse(weeklyData);
        }

        @Transactional(readOnly = true)
        public AdminItemUsageStatsResponse getItemUsageStats() {
                List<Object[]> statsData = itemHistoryRepository.findItemUsageStats();

                List<AdminItemUsageStatsResponse.ItemUsageStat> itemStats = statsData.stream()
                                .map(row -> new AdminItemUsageStatsResponse.ItemUsageStat(
                                                (String) row[0],
                                                row[1].toString(),
                                                ((Number) row[2]).longValue(),
                                                ((Number) row[3]).longValue()))
                                .collect(Collectors.toList());

                long attendanceCount = coinHistoryRepository.countByTypeAndCreatedAtBetween(
                                CoinLogType.ATTENDANCE,
                                LocalDateTime.of(2000, 1, 1, 0, 0),
                                LocalDateTime.now());

                long watermelonCount = coinHistoryRepository.countByTypeAndCreatedAtBetween(
                                CoinLogType.WATERMELON,
                                LocalDateTime.of(2000, 1, 1, 0, 0),
                                LocalDateTime.now());

                return new AdminItemUsageStatsResponse(itemStats, attendanceCount, watermelonCount);
        }
}
