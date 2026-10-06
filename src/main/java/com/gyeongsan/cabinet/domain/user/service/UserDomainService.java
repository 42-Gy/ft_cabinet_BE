package com.gyeongsan.cabinet.domain.user.service;

import com.gyeongsan.cabinet.adapter.in.web.user.dto.MyProfileResponseDto;
import com.gyeongsan.cabinet.domain.cabinet.model.Cabinet;
import com.gyeongsan.cabinet.domain.coin.model.CoinHistory;
import com.gyeongsan.cabinet.domain.coin.model.CoinLogType;
import com.gyeongsan.cabinet.domain.coin.port.out.CoinHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.item.model.Item;
import com.gyeongsan.cabinet.domain.item.model.ItemHistory;
import com.gyeongsan.cabinet.domain.item.port.out.ItemHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.model.LentHistory;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.ReservationPort;
import com.gyeongsan.cabinet.domain.user.model.Attendance;
import com.gyeongsan.cabinet.domain.user.model.FtGradeResolver;
import com.gyeongsan.cabinet.domain.user.model.FtGradeSnapshot;
import com.gyeongsan.cabinet.domain.user.model.LentTicketRewardPolicy;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.port.in.UserUseCase;
import com.gyeongsan.cabinet.domain.user.port.out.AttendanceRepositoryPort;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Log4j2
public class UserDomainService implements UserUseCase {

    private final UserRepositoryPort userRepository;
    private final LentRepositoryPort lentRepository;
    private final ItemHistoryRepositoryPort itemHistoryRepository;
    private final AttendanceRepositoryPort attendanceRepository;
    private final CoinHistoryRepositoryPort coinHistoryRepository;
    private final ReservationPort reservationPort;
    private final LentTicketRewardPolicy rewardPolicy;

    @Override
    public MyProfileResponseDto getMyProfile(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        LentHistory activeLent = lentRepository.findByUserIdAndEndedAtIsNull(userId).orElse(null);
        List<ItemHistory> myItems = itemHistoryRepository.findAllByUserIdAndUsedAtIsNull(userId);

        if (myItems == null) {
            myItems = Collections.emptyList();
        }

        List<MyProfileResponseDto.MyItemDto> itemDtos =
                myItems.stream()
                        .map(
                                item -> {
                                    String typeStr =
                                            (item.getItem() != null
                                                            && item.getItem().getType() != null)
                                                    ? item.getItem().getType().name()
                                                    : "UNKNOWN";

                                    return MyProfileResponseDto.MyItemDto.builder()
                                            .itemHistoryId(item.getId())
                                            .itemName(
                                                    item.getItem() != null
                                                            ? item.getItem().getName()
                                                            : "알 수 없음")
                                            .itemType(typeStr)
                                            .purchaseAt(item.getPurchaseAt())
                                            .build();
                                })
                        .collect(Collectors.toList());

        Long cabinetId = null;
        Integer visibleNum = null;
        String section = null;
        String lentStartedAt = null;
        String expiredAt = null;
        LocalDateTime expiredAtIso = null;
        Integer daysRemaining = null;
        Boolean overdue = null;
        String previousPassword = null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM월 dd일 HH:mm");

        if (activeLent != null && activeLent.getCabinet() != null) {
            Cabinet cabinet = activeLent.getCabinet();
            cabinetId = cabinet.getId();
            visibleNum = cabinet.getVisibleNum();
            section = cabinet.getSection();

            lentStartedAt = activeLent.getStartedAt().format(formatter);
            expiredAt = activeLent.getExpiredAt().format(formatter);

            LocalDateTime now = LocalDateTime.now();
            expiredAtIso = activeLent.getExpiredAt();
            daysRemaining = activeLent.calculateRemainingDays(now.toLocalDate());
            overdue = activeLent.isOverdue(now);

            LentHistory prevHistory =
                    lentRepository
                            .findTopByCabinetIdAndEndedAtIsNotNullOrderByEndedAtDesc(
                                    cabinet.getId())
                            .orElse(null);

            if (prevHistory != null) {
                previousPassword = prevHistory.getReturnMemo();
            }
        }

        Integer penaltyDays = user.getPenaltyDays();
        if (penaltyDays == null) penaltyDays = 0;

        Boolean autoExtensionEnabled = false;
        if (activeLent != null) {
            autoExtensionEnabled = activeLent.isAutoExtension();
        }

        Integer reservedVisibleNum = null;
        Long reservationRemainingSeconds = null;
        try {
            Optional<Integer> reserved = reservationPort.getUserReservation(userId);
            Optional<Long> remaining = reservationPort.getUserReservationTtlSeconds(userId);
            // 두 값을 읽는 사이 예약이 만료되었다면 예약이 없는 것으로 본다.
            if (reserved.isPresent() && remaining.isPresent()) {
                reservedVisibleNum = reserved.get();
                reservationRemainingSeconds = remaining.get();
            }
        } catch (RuntimeException e) {
            // 예약 정보는 보조 정보이므로, 조회에 실패해도 프로필 전체를 막지 않는다.
            log.warn("내 예약 정보 조회 실패 - User: {}, 원인: {}", userId, e.toString());
        }

        return MyProfileResponseDto.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .coin(user.getCoin())
                .role(user.getRole())
                .isPisciner(user.isPisciner())
                .transcender(user.isTranscender())
                .penaltyDays(penaltyDays)
                .monthlyLogtime(user.getMonthlyLogtime())
                .lentCabinetId(cabinetId)
                .visibleNum(visibleNum)
                .section(section)
                .autoExtensionEnabled(autoExtensionEnabled)
                .lentStartedAt(lentStartedAt)
                .reservedVisibleNum(reservedVisibleNum)
                .reservationRemainingSeconds(reservationRemainingSeconds)
                .expiredAt(expiredAt)
                .expiredAtIso(expiredAtIso)
                .daysRemaining(daysRemaining)
                .overdue(overdue)
                .previousPassword(previousPassword)
                .myItems(itemDtos)
                .coinHistories(
                        coinHistoryRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                                .map(
                                        ch ->
                                                MyProfileResponseDto.CoinHistoryDto.builder()
                                                        .date(
                                                                ch.getCreatedAt()
                                                                        .toLocalDate()
                                                                        .toString())
                                                        .amount(ch.getAmount())
                                                        .type(ch.getAmount() > 0 ? "EARN" : "SPEND")
                                                        .reason(
                                                                ch.getDescription() != null
                                                                        ? ch.getDescription()
                                                                        : ch.getType().name())
                                                        .build())
                                .collect(Collectors.toList()))
                .itemHistories(
                        itemHistoryRepository.findAllByUserIdOrderByPurchaseAtDesc(userId).stream()
                                .map(
                                        ih ->
                                                MyProfileResponseDto.ItemHistoryDto.builder()
                                                        .date(
                                                                ih.getPurchaseAt()
                                                                        .toLocalDate()
                                                                        .toString())
                                                        .itemName(ih.getItem().getName())
                                                        .itemType(ih.getItem().getType().name())
                                                        .status(
                                                                ih.getUsedAt() != null
                                                                        ? "USED"
                                                                        : "UNUSED")
                                                        .usedAt(
                                                                ih.getUsedAt() != null
                                                                        ? ih.getUsedAt()
                                                                                .toLocalDate()
                                                                                .toString()
                                                                        : null)
                                                        .build())
                                .collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional
    public void doAttendance(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("유저가 없습니다."));

        LocalDate today = LocalDate.now();

        if (attendanceRepository.findTodayAttendance(user, today, today).isPresent()) {
            throw new IllegalStateException("이미 오늘 출석체크를 완료했습니다.");
        }

        Attendance attendance = new Attendance(user, today);
        attendanceRepository.save(attendance);

        user.addCoin(100L);
        CoinHistory attendanceReward = CoinHistory.of(user, 100L, CoinLogType.ATTENDANCE, "출석 보상");
        coinHistoryRepository.save(attendanceReward);

        LocalDate startOfMonth = today.withDayOfMonth(1);
        long attendanceCount =
                attendanceRepository.countLoginDaysByUserId(userId, startOfMonth, today);

        if (attendanceCount == 20) {
            user.addCoin(2000L);
            CoinHistory watermelonReward =
                    CoinHistory.of(user, 2000L, CoinLogType.WATERMELON, "월간 만근 보상 (황금 수박씨)");
            coinHistoryRepository.save(watermelonReward);
            log.info(
                    "[Golden Watermelon] {}님 이번 달 20번째 출석 달성! 2000 씨앗 추가 지급! (총 출석: {}일)",
                    user.getName(),
                    attendanceCount);
        } else {
            log.info("{}님 오늘 출석 완료. (이번 달 {}일째)", user.getName(), attendanceCount);
        }
    }

    @Override
    public List<LocalDate> getMyAttendanceDates(Long userId) {
        return attendanceRepository.findAllByUserId(userId).stream()
                .map(Attendance::getAttendanceDate)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processLogtimeTransaction(
            Long userId, Item lentTicketItem, int totalMinutes, boolean isPayDay) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        if (totalMinutes >= 0) {
            user.updateMonthlyLogtime(totalMinutes);
            log.info("User {}: 이번 달 누적 시간 업데이트 -> {}분", user.getName(), totalMinutes);
        }

        if (isPayDay) {
            // 사용자마다 적용되는 기준은 하나뿐이라, 두 기준을 동시에 만족해도 대여권은 많아야 1개다.
            if (lentTicketItem != null && rewardPolicy.qualifies(user, user.getMonthlyLogtime())) {
                int currentLentCount =
                        itemHistoryRepository.countByUserIdAndItemTypeAndUsedAtIsNull(
                                userId, lentTicketItem.getType());
                if (currentLentCount < 1) {
                    ItemHistory reward =
                            new ItemHistory(LocalDateTime.now(), null, user, lentTicketItem);
                    itemHistoryRepository.save(reward);
                    log.info(
                            "[Reward] {}님 지난달 {}분 달성(기준 {}분{}). 대여권 지급 완료.",
                            user.getName(),
                            user.getMonthlyLogtime(),
                            rewardPolicy.thresholdMinutesFor(user),
                            user.isTranscender() ? ", 트센" : "");
                } else {
                    log.info("[Skip] {}님 대여권 이미 보유 중 (최대 1개). 지급 생략.", user.getName());
                }
            }
            user.resetMonthlyLogtime();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean needsGradeRefresh(Long userId, int totalMinutes) {
        return userRepository
                .findById(userId)
                .map(user -> rewardPolicy.isGradeRefreshCandidate(user, totalMinutes))
                .orElse(false);
    }

    @Override
    @Transactional
    public void updateFtGrade(Long userId, FtGradeSnapshot snapshot) {
        if (!snapshot.parsed()) {
            return;
        }
        if (snapshot.rejected()) {
            log.warn("[Grade] userId={} 의 grade 형식이 올바르지 않아 저장하지 않습니다.", userId);
            return;
        }
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("유저 없음"));
        if (!FtGradeResolver.isRecognized(snapshot.grade())) {
            log.warn("[Grade] 미확인 grade 값을 받았습니다(일반 사용자로 취급): {}", snapshot.grade());
        }
        user.updateFtGrade(snapshot.grade());
    }
}
