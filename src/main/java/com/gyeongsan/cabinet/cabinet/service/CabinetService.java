package com.gyeongsan.cabinet.cabinet.service;

import com.gyeongsan.cabinet.cabinet.domain.Cabinet;
import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;
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

    // 1. 사물함 목록 조회 (개별 정보)
    public List<CabinetListResponseDto> getCabinetList(Integer floor) {
        List<Cabinet> cabinets = cabinetRepository.findAllByFloor(floor);
        List<Long> cabinetIds = cabinets.stream().map(Cabinet::getId).collect(Collectors.toList());

        // N+1 문제 방지: 모든 활성 대여 기록을 한 번에 조회
        List<LentHistory> activeLents = lentRepository.findAllActiveLentByCabinetIds(cabinetIds);

        return cabinets.stream()
                .map(cabinet -> {
                    // 현재 사물함에 해당하는 대여 기록 찾기
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

                        // 남은 일자 계산
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

                            // 대여 정보
                            .lentUserName(userName)
                            .lentStartedAt(startedAt)
                            .lentExpiredAt(expiredAt)
                            .daysRemaining(daysRemaining)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // 2. 사물함 현황 통계 조회 (섹션별 개수)
    public List<CabinetStatusDto> getStatusSummaryByFloor(Integer floor) {
        List<Cabinet> cabinets = cabinetRepository.findAllByFloor(floor);

        // 섹션별로 그룹화하여 통계 계산
        return cabinets.stream()
                .collect(Collectors.groupingBy(Cabinet::getSection))
                .entrySet().stream()
                .map(entry -> {
                    String sectionName = entry.getKey();
                    List<Cabinet> sectionCabinets = entry.getValue();

                    long total = sectionCabinets.size();

                    // 상태별 카운트
                    long available = sectionCabinets.stream()
                            .filter(c -> c.getStatus() == CabinetStatus.AVAILABLE)
                            .count();
                    long full = sectionCabinets.stream()
                            .filter(c -> c.getStatus() == CabinetStatus.FULL || c.getStatus() == CabinetStatus.OVERDUE)
                            .count();
                    long broken = sectionCabinets.stream()
                            // 👇 [수정] CabinetStatus.DISABLED가 Enum에 추가되어 이제 인식됨
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
                // 섹션 이름 순으로 정렬
                .sorted((a, b) -> a.getSection().compareTo(b.getSection()))
                .collect(Collectors.toList());
    }
}