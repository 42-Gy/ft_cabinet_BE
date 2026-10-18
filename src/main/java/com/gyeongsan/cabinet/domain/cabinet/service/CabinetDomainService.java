package com.gyeongsan.cabinet.domain.cabinet.service;

import com.gyeongsan.cabinet.adapter.in.web.cabinet.dto.BuildingStatusDto;
import com.gyeongsan.cabinet.adapter.in.web.cabinet.dto.CabinetDetailResponseDto;
import com.gyeongsan.cabinet.adapter.in.web.cabinet.dto.CabinetListResponseDto;
import com.gyeongsan.cabinet.adapter.in.web.cabinet.dto.CabinetStatusDto;
import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import com.gyeongsan.cabinet.domain.cabinet.model.Cabinet;
import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import com.gyeongsan.cabinet.domain.cabinet.port.in.CabinetQueryUseCase;
import com.gyeongsan.cabinet.domain.cabinet.port.out.CabinetRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.model.LentHistory;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.ReservationPort;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Log4j2
public class CabinetDomainService implements CabinetQueryUseCase {

    private final CabinetRepositoryPort cabinetRepository;
    private final LentRepositoryPort lentRepository;
    private final ReservationPort reservationPort;

    @Override
    public List<CabinetListResponseDto> getCabinetList(Integer floor, UserPrincipal userPrincipal) {
        List<Cabinet> cabinets = cabinetRepository.findAllByFloor(floor);
        List<Long> cabinetIds = cabinets.stream().map(Cabinet::getId).collect(Collectors.toList());

        List<LentHistory> activeLents = lentRepository.findAllActiveLentByCabinetIds(cabinetIds);
        Map<Integer, Long> reservedUserIds = findReservedUserIds(cabinets);

        return cabinets.stream()
                .map(
                        cabinet -> {
                            LentHistory activeLent =
                                    activeLents.stream()
                                            .filter(
                                                    lent ->
                                                            lent.getCabinet()
                                                                    .getId()
                                                                    .equals(cabinet.getId()))
                                            .findFirst()
                                            .orElse(null);

                            String userName = null;
                            LocalDateTime startedAt = null;
                            LocalDateTime expiredAt = null;
                            long daysRemaining = 0;

                            if (activeLent != null) {
                                userName = activeLent.getUser().getName();
                                startedAt = activeLent.getStartedAt();
                                expiredAt = activeLent.getExpiredAt();
                                daysRemaining =
                                        expiredAt != null
                                                ? ChronoUnit.DAYS.between(
                                                        LocalDateTime.now(), expiredAt)
                                                : 0;
                            }

                            if (userPrincipal == null && userName != null) {
                                userName = "*****";
                            }

                            Long reservedBy = reservedUserIds.get(cabinet.getVisibleNum());

                            return CabinetListResponseDto.builder()
                                    .cabinetId(cabinet.getId())
                                    .visibleNum(cabinet.getVisibleNum())
                                    .floor(cabinet.getFloor())
                                    .section(cabinet.getSection())
                                    .lentType(cabinet.getLentType().name())
                                    .status(cabinet.getStatus())
                                    .statusNote(cabinet.getStatusNote())
                                    .lentUserName(userName)
                                    .lentStartedAt(startedAt)
                                    .lentExpiredAt(expiredAt)
                                    .daysRemaining(daysRemaining)
                                    .isReserved(reservedBy != null)
                                    .isReservedByMe(isMine(reservedBy, userPrincipal))
                                    .build();
                        })
                .collect(Collectors.toList());
    }

    private Map<Integer, Long> findReservedUserIds(List<Cabinet> cabinets) {
        Set<Integer> availableNums =
                cabinets.stream()
                        .filter(c -> c.getStatus() == CabinetStatus.AVAILABLE)
                        .map(Cabinet::getVisibleNum)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        if (availableNums.isEmpty()) {
            return Map.of();
        }
        try {
            return reservationPort.getReservedUserIds(availableNums);
        } catch (RuntimeException e) {
            log.warn("사물함 예약 목록 조회 실패 - 원인: {}", e.toString());
            return Map.of();
        }
    }

    private static boolean isMine(Long reservedBy, UserPrincipal userPrincipal) {
        return reservedBy != null
                && userPrincipal != null
                && reservedBy.equals(userPrincipal.getUserId());
    }

    @Override
    public List<CabinetStatusDto> getStatusSummaryByFloor(Integer floor) {
        List<Cabinet> cabinets = cabinetRepository.findAllByFloor(floor);
        return calculateStatus(cabinets);
    }

    @Override
    public BuildingStatusDto getBuildingStatus() {
        List<Cabinet> allCabinets = cabinetRepository.findAll();

        long total = allCabinets.size();
        long available =
                allCabinets.stream().filter(c -> c.getStatus() == CabinetStatus.AVAILABLE).count();
        long full =
                allCabinets.stream()
                        .filter(
                                c ->
                                        c.getStatus() == CabinetStatus.FULL
                                                || c.getStatus() == CabinetStatus.OVERDUE)
                        .count();
        long broken =
                allCabinets.stream()
                        .filter(
                                c ->
                                        c.getStatus() == CabinetStatus.BROKEN
                                                || c.getStatus() == CabinetStatus.DISABLED)
                        .count();

        return BuildingStatusDto.builder()
                .totalCounts(total)
                .totalAvailable(available)
                .totalFull(full)
                .totalBroken(broken)
                .build();
    }

    @Override
    public CabinetDetailResponseDto getCabinetDetail(Long cabinetId, UserPrincipal userPrincipal) {
        Cabinet cabinet =
                cabinetRepository
                        .findById(cabinetId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사물함입니다."));

        LentHistory activeLent =
                lentRepository.findByCabinetIdAndEndedAtIsNull(cabinetId).orElse(null);
        LentHistory previousLent =
                lentRepository
                        .findTopByCabinetIdAndEndedAtIsNotNullOrderByEndedAtDesc(cabinetId)
                        .orElse(null);

        String curName = (activeLent != null) ? activeLent.getUser().getName() : null;
        LocalDateTime curStart = (activeLent != null) ? activeLent.getStartedAt() : null;
        LocalDateTime curEnd = (activeLent != null) ? activeLent.getExpiredAt() : null;

        String prevName = (previousLent != null) ? previousLent.getUser().getName() : "-";
        LocalDateTime prevEnd = (previousLent != null) ? previousLent.getEndedAt() : null;

        if (userPrincipal == null) {
            if (curName != null) curName = "*****";
            if (!"-".equals(prevName)) prevName = "*****";
        }

        boolean isReserved = false;
        boolean isReservedByMe = false;
        Long reservationRemainingSeconds = null;
        try {
            Optional<Long> reservedUserId =
                    reservationPort.getReservedUserId(cabinet.getVisibleNum());
            Optional<Long> remaining =
                    reservedUserId.isPresent()
                            ? reservationPort.getCabinetReservationTtlSeconds(
                                    cabinet.getVisibleNum())
                            : Optional.empty();
            isReserved = reservedUserId.isPresent();
            isReservedByMe = isMine(reservedUserId.orElse(null), userPrincipal);
            reservationRemainingSeconds = remaining.orElse(null);
        } catch (RuntimeException e) {
            log.warn("사물함 예약 상태 조회 실패 - Cabinet: {}, 원인: {}", cabinetId, e.toString());
        }

        return CabinetDetailResponseDto.builder()
                .cabinetId(cabinet.getId())
                .visibleNum(cabinet.getVisibleNum())
                .floor(cabinet.getFloor())
                .section(cabinet.getSection())
                .status(cabinet.getStatus())
                .statusNote(cabinet.getStatusNote())
                .lentUserName(curName)
                .lentStartedAt(curStart)
                .lentExpiredAt(curEnd)
                .previousUserName(prevName)
                .previousEndedAt(prevEnd)
                .isReserved(isReserved)
                .isReservedByMe(isReservedByMe)
                .reservationRemainingSeconds(reservationRemainingSeconds)
                .build();
    }

    private List<CabinetStatusDto> calculateStatus(List<Cabinet> cabinets) {
        return cabinets.stream()
                .collect(Collectors.groupingBy(Cabinet::getSection))
                .entrySet()
                .stream()
                .map(
                        entry -> {
                            String sectionName = entry.getKey();
                            List<Cabinet> sectionCabinets = entry.getValue();

                            long total = sectionCabinets.size();
                            long available =
                                    sectionCabinets.stream()
                                            .filter(c -> c.getStatus() == CabinetStatus.AVAILABLE)
                                            .count();
                            long full =
                                    sectionCabinets.stream()
                                            .filter(
                                                    c ->
                                                            c.getStatus() == CabinetStatus.FULL
                                                                    || c.getStatus()
                                                                            == CabinetStatus
                                                                                    .OVERDUE)
                                            .count();
                            long broken =
                                    sectionCabinets.stream()
                                            .filter(
                                                    c ->
                                                            c.getStatus() == CabinetStatus.BROKEN
                                                                    || c.getStatus()
                                                                            == CabinetStatus
                                                                                    .DISABLED)
                                            .count();

                            return CabinetStatusDto.builder()
                                    .section(sectionName)
                                    .total(total)
                                    .availableCount(available)
                                    .fullCount(full)
                                    .brokenCount(broken)
                                    .build();
                        })
                .sorted(
                        Comparator.comparing(
                                CabinetStatusDto::getSection,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }
}
