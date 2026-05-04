package com.gyeongsan.cabinet.domain.cabinet.service;

import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import com.gyeongsan.cabinet.cabinet.domain.Cabinet;
import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;
import com.gyeongsan.cabinet.cabinet.dto.BuildingStatusDto;
import com.gyeongsan.cabinet.cabinet.dto.CabinetDetailResponseDto;
import com.gyeongsan.cabinet.cabinet.dto.CabinetListResponseDto;
import com.gyeongsan.cabinet.cabinet.dto.CabinetStatusDto;
import com.gyeongsan.cabinet.domain.cabinet.port.in.CabinetQueryUseCase;
import com.gyeongsan.cabinet.domain.cabinet.port.out.CabinetRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.ReservationPort;
import com.gyeongsan.cabinet.lent.domain.LentHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CabinetDomainService implements CabinetQueryUseCase {

    private final CabinetRepositoryPort cabinetRepository;
    private final LentRepositoryPort lentRepository;
    private final ReservationPort reservationPort;

    @Override
    public List<CabinetListResponseDto> getCabinetList(Integer floor, UserPrincipal userPrincipal) {
        List<Cabinet> cabinets = cabinetRepository.findAllByFloor(floor);
        List<Long> cabinetIds = cabinets.stream().map(Cabinet::getId).collect(Collectors.toList());

        List<LentHistory> activeLents = lentRepository.findAllActiveLentByCabinetIds(cabinetIds);

        return cabinets.stream()
                .map(cabinet -> {
                    LentHistory activeLent = activeLents.stream()
                            .filter(lent -> lent.getCabinet().getId().equals(cabinet.getId()))
                            .findFirst().orElse(null);

                    String userName = null;
                    LocalDateTime startedAt = null;
                    LocalDateTime expiredAt = null;
                    long daysRemaining = 0;

                    if (activeLent != null) {
                        userName = activeLent.getUser().getName();
                        startedAt = activeLent.getStartedAt();
                        expiredAt = activeLent.getExpiredAt();
                        daysRemaining = expiredAt != null
                                ? ChronoUnit.DAYS.between(LocalDateTime.now(), expiredAt)
                                : 0;
                    }

                    if (userPrincipal == null && userName != null) {
                        userName = "*****";
                    }

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
                            .build();
                })
                .collect(Collectors.toList());
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
        long available = allCabinets.stream().filter(c -> c.getStatus() == CabinetStatus.AVAILABLE).count();
        long full = allCabinets.stream().filter(c -> c.getStatus() == CabinetStatus.FULL || c.getStatus() == CabinetStatus.OVERDUE).count();
        long broken = allCabinets.stream().filter(c -> c.getStatus() == CabinetStatus.BROKEN || c.getStatus() == CabinetStatus.DISABLED).count();

        return BuildingStatusDto.builder()
                .totalCounts(total)
                .totalAvailable(available)
                .totalFull(full)
                .totalBroken(broken)
                .build();
    }

    @Override
    public CabinetDetailResponseDto getCabinetDetail(Long cabinetId, UserPrincipal userPrincipal) {
        Cabinet cabinet = cabinetRepository.findById(cabinetId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사물함입니다."));

        LentHistory activeLent = lentRepository.findByCabinetIdAndEndedAtIsNull(cabinetId).orElse(null);
        LentHistory previousLent = lentRepository.findTopByCabinetIdAndEndedAtIsNotNullOrderByEndedAtDesc(cabinetId).orElse(null);

        String curName = (activeLent != null) ? activeLent.getUser().getName() : null;
        LocalDateTime curStart = (activeLent != null) ? activeLent.getStartedAt() : null;
        LocalDateTime curEnd = (activeLent != null) ? activeLent.getExpiredAt() : null;

        String prevName = (previousLent != null) ? previousLent.getUser().getName() : "-";
        LocalDateTime prevEnd = (previousLent != null) ? previousLent.getEndedAt() : null;

        if (userPrincipal == null) {
            if (curName != null) curName = "*****";
            if (!"-".equals(prevName)) prevName = "*****";
        }

        Boolean isReservedByMe = false;
        if (userPrincipal != null) {
            var reservedUserId = reservationPort.getReservedUserId(cabinet.getVisibleNum());
            if (reservedUserId.isPresent() && reservedUserId.get().equals(userPrincipal.getUserId())) {
                isReservedByMe = true;
            }
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
                .isReservedByMe(isReservedByMe)
                .build();
    }

    private List<CabinetStatusDto> calculateStatus(List<Cabinet> cabinets) {
        return cabinets.stream()
                .collect(Collectors.groupingBy(Cabinet::getSection))
                .entrySet().stream()
                .map(entry -> {
                    String sectionName = entry.getKey();
                    List<Cabinet> sectionCabinets = entry.getValue();

                    long total = sectionCabinets.size();
                    long available = sectionCabinets.stream().filter(c -> c.getStatus() == CabinetStatus.AVAILABLE).count();
                    long full = sectionCabinets.stream().filter(c -> c.getStatus() == CabinetStatus.FULL || c.getStatus() == CabinetStatus.OVERDUE).count();
                    long broken = sectionCabinets.stream().filter(c -> c.getStatus() == CabinetStatus.BROKEN || c.getStatus() == CabinetStatus.DISABLED).count();

                    return CabinetStatusDto.builder()
                            .section(sectionName)
                            .total(total)
                            .availableCount(available)
                            .fullCount(full)
                            .brokenCount(broken)
                            .build();
                })
                .sorted(Comparator.comparing(CabinetStatusDto::getSection, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }
}
