package com.gyeongsan.cabinet.cabinet.service;

import com.gyeongsan.cabinet.cabinet.domain.Cabinet;
import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;
import com.gyeongsan.cabinet.cabinet.dto.BuildingStatusDto;
import com.gyeongsan.cabinet.cabinet.dto.CabinetDetailResponseDto;
import com.gyeongsan.cabinet.cabinet.dto.CabinetListResponseDto;
import com.gyeongsan.cabinet.cabinet.dto.CabinetStatusDto;
import com.gyeongsan.cabinet.cabinet.repository.CabinetRepository;
import com.gyeongsan.cabinet.lent.domain.LentHistory;
import com.gyeongsan.cabinet.lent.repository.LentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CabinetService {

    private final CabinetRepository cabinetRepository;
    private final LentRepository lentRepository;

    public List<CabinetListResponseDto> getCabinetList(Integer floor) {
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

    public List<CabinetStatusDto> getStatusSummaryByFloor(Integer floor) {
        List<Cabinet> cabinets = cabinetRepository.findAllByFloor(floor);
        return calculateStatus(cabinets);
    }

    public BuildingStatusDto getBuildingStatus() {
        List<Cabinet> allCabinets = cabinetRepository.findAll();

        long total = allCabinets.size();

        long available = allCabinets.stream()
                .filter(c -> c.getStatus() == CabinetStatus.AVAILABLE)
                .count();

        long full = allCabinets.stream()
                .filter(c -> c.getStatus() == CabinetStatus.FULL || c.getStatus() == CabinetStatus.OVERDUE)
                .count();

        long broken = allCabinets.stream()
                .filter(c -> c.getStatus() == CabinetStatus.BROKEN || c.getStatus() == CabinetStatus.DISABLED)
                .count();

        return BuildingStatusDto.builder()
                .totalCounts(total)
                .totalAvailable(available)
                .totalFull(full)
                .totalBroken(broken)
                .build();
    }

    public CabinetDetailResponseDto getCabinetDetail(Long cabinetId) {
        Cabinet cabinet = cabinetRepository.findById(cabinetId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사물함입니다."));

        LentHistory activeLent = lentRepository.findByCabinetIdAndEndedAtIsNull(cabinetId).orElse(null);
        LentHistory previousLent = lentRepository.findTopByCabinetIdAndEndedAtIsNotNullOrderByEndedAtDesc(cabinetId).orElse(null);

        String curName = (activeLent != null) ? activeLent.getUser().getName() : null;
        LocalDateTime curStart = (activeLent != null) ? activeLent.getStartedAt() : null;
        LocalDateTime curEnd = (activeLent != null) ? activeLent.getExpiredAt() : null;

        String prevName = (previousLent != null) ? previousLent.getUser().getName() : "-";
        LocalDateTime prevEnd = (previousLent != null) ? previousLent.getEndedAt() : null;

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

                    long available = sectionCabinets.stream()
                            .filter(c -> c.getStatus() == CabinetStatus.AVAILABLE)
                            .count();
                    long full = sectionCabinets.stream()
                            .filter(c -> c.getStatus() == CabinetStatus.FULL || c.getStatus() == CabinetStatus.OVERDUE)
                            .count();
                    long broken = sectionCabinets.stream()
                            .filter(c -> c.getStatus() == CabinetStatus.BROKEN || c.getStatus() == CabinetStatus.DISABLED)
                            .count();

                    return CabinetStatusDto.builder()
                            .section(sectionName)
                            .total(total)
                            .availableCount(available)
                            .fullCount(full)
                            .brokenCount(broken)
                            .build();
                })
                .sorted((a, b) -> a.getSection().compareTo(b.getSection()))
                .collect(Collectors.toList());
    }
}
