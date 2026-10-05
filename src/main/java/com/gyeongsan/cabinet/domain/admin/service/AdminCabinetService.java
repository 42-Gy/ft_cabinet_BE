package com.gyeongsan.cabinet.domain.admin.service;

import com.gyeongsan.cabinet.adapter.in.web.admin.dto.*;
import com.gyeongsan.cabinet.domain.admin.port.in.AdminCabinetUseCase;
import com.gyeongsan.cabinet.domain.cabinet.model.Cabinet;
import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import com.gyeongsan.cabinet.domain.cabinet.model.LentType;
import com.gyeongsan.cabinet.domain.cabinet.port.out.CabinetRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.model.LentHistory;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.global.exception.BulkStatusUpdateRejectedException;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class AdminCabinetService implements AdminCabinetUseCase {

    /** CABINET.STATUS_NOTE 컬럼 길이. 넘기면 DB 에서 저장이 실패한다. */
    private static final int STATUS_NOTE_MAX_LENGTH = 64;

    /** 대여를 끝내는 것이 의미 있는 목표 상태. FULL/OVERDUE 는 대여 중이라는 뜻의 파생 상태라 제외한다. */
    private static final Set<CabinetStatus> LENT_ENDING_STATUSES =
            EnumSet.of(
                    CabinetStatus.AVAILABLE,
                    CabinetStatus.BROKEN,
                    CabinetStatus.DISABLED,
                    CabinetStatus.PENDING);

    private final CabinetRepositoryPort cabinetRepository;
    private final LentRepositoryPort lentRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<CabinetHistoryResponse> getCabinetHistory(Integer visibleNum, Pageable pageable) {
        return lentRepository
                .findHistoryByCabinet(visibleNum, pageable)
                .map(CabinetHistoryResponse::from);
    }

    @Override
    public void updateCabinetStatus(Integer visibleNum, CabinetStatusRequest request) {
        Cabinet cabinet =
                cabinetRepository
                        .findByVisibleNumWithLock(visibleNum)
                        .orElseThrow(() -> new ServiceException(ErrorCode.CABINET_NOT_FOUND));

        cabinet.updateStatus(request.status(), request.lentType(), request.statusNote());
    }

    @Override
    public void forceReturn(Integer visibleNum) {
        Cabinet cabinet =
                cabinetRepository
                        .findByVisibleNumWithLock(visibleNum)
                        .orElseThrow(() -> new ServiceException(ErrorCode.CABINET_NOT_FOUND));

        LentHistory activeLent =
                lentRepository.findByCabinetIdAndEndedAtIsNull(cabinet.getId()).orElse(null);

        if (activeLent != null) {
            activeLent.endLent(LocalDateTime.now(), "관리자 강제 반납");
        }

        cabinet.updateStatus(CabinetStatus.PENDING, cabinet.getLentType(), "강제 반납: 물품 수거 및 청소 필요");
    }

    @Override
    @Transactional(readOnly = true)
    public List<CabinetPendingResponseDto> getPendingCabinets() {
        List<LentHistory> pendingLentHistories =
                lentRepository.findAllLatestLentForPendingCabinets();

        return pendingLentHistories.stream()
                .map(
                        lh ->
                                new CabinetPendingResponseDto(
                                        lh.getCabinet().getVisibleNum(),
                                        lh.getCabinet().getStatusNote(),
                                        lh.getCabinet().getLentType(),
                                        lh.getPhotoUrl(),
                                        lh.getUser().getName()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReturnPhotoResponseDto> getReturnPhotos(Pageable pageable) {
        return lentRepository
                .findAllReturnedWithPhoto(pageable)
                .map(
                        lh ->
                                new ReturnPhotoResponseDto(
                                        lh.getId(),
                                        lh.getCabinet().getVisibleNum(),
                                        lh.getUser().getName(),
                                        lh.getPhotoUrl(),
                                        lh.getEndedAt(),
                                        lh.getReturnMemo()));
    }

    @Override
    public void approveManualReturn(Integer visibleNum) {
        Cabinet cabinet =
                cabinetRepository
                        .findByVisibleNumWithLock(visibleNum)
                        .orElseThrow(() -> new ServiceException(ErrorCode.CABINET_NOT_FOUND));

        if (cabinet.getStatus() != CabinetStatus.PENDING) {
            throw new ServiceException(ErrorCode.INVALID_CABINET_STATUS);
        }

        cabinet.updateStatus(CabinetStatus.AVAILABLE, cabinet.getLentType(), null);
    }

    @Override
    @Transactional(readOnly = true)
    public CabinetDetailResponse getCabinetDetail(Integer visibleNum) {
        Cabinet cabinet =
                cabinetRepository
                        .findByVisibleNum(visibleNum)
                        .orElseThrow(() -> new ServiceException(ErrorCode.CABINET_NOT_FOUND));

        LentHistory activeLent =
                lentRepository.findByCabinetIdAndEndedAtIsNull(cabinet.getId()).orElse(null);

        return CabinetDetailResponse.of(cabinet, activeLent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrokenCabinetResponse> getBrokenCabinets() {
        return cabinetRepository.findAllByStatus(CabinetStatus.BROKEN).stream()
                .map(BrokenCabinetResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 사물함 상태/LentType/사유를 일괄 변경한다. 요청 전체가 하나의 트랜잭션이며, 문제가 있으면 아무것도 바꾸지 않는다.
     *
     * <ul>
     *   <li>대상 ID 가 하나라도 존재하지 않으면 전체 거부
     *   <li>status 를 바꾸는데 대여 중인 사물함이 있고 endActiveLents 가 false 면 전체 거부
     *   <li>endActiveLents=true 일 때만 해당 사물함의 활성 대여를 종료
     * </ul>
     */
    @Override
    public BulkStatusUpdateResponse bulkUpdateCabinetStatus(
            BulkStatusUpdateRequest request, String actor) {
        String batchId = UUID.randomUUID().toString();
        log.info(
                "[BULK:{}] 일괄 변경 요청 - 관리자: {}, 대상: {}건, status: {}, lentType: {}, endActiveLents: {}, 사유: {}",
                batchId,
                actor,
                request.cabinetIds() == null ? 0 : request.cabinetIds().size(),
                request.status(),
                request.lentType(),
                request.endActiveLents(),
                sanitizeForLog(request.statusNote()));

        validateBulkRequest(request);

        // 오름차순으로 락을 잡아야, 서로 다른 일괄 요청이 반대 순서로 락을 쥐는 데드락이 생기지 않는다.
        List<Long> sortedIds = request.cabinetIds().stream().distinct().sorted().toList();
        List<Cabinet> cabinets = new ArrayList<>();
        List<Long> missingIds = new ArrayList<>();
        for (Long id : sortedIds) {
            cabinetRepository
                    .findByIdWithLock(id)
                    .ifPresentOrElse(cabinets::add, () -> missingIds.add(id));
        }

        // 락을 잡은 뒤에 대여를 조회해야, 대여/예약이 끼어들어도 이 시점의 상태가 확정된다.
        Map<Long, List<LentHistory>> activeLentsByCabinetId =
                loadActiveLents(cabinets, request.status());

        List<BulkStatusRejection.OccupiedCabinet> occupiedCabinets =
                request.endActiveLents()
                        ? List.of()
                        : findOccupiedCabinets(cabinets, activeLentsByCabinetId);
        if (!missingIds.isEmpty() || !occupiedCabinets.isEmpty()) {
            log.warn(
                    "[BULK:{}] 거부 - 존재하지 않는 ID: {}, 대여 중인 사물함: {}",
                    batchId,
                    missingIds,
                    occupiedCabinets.stream().map(o -> o.visibleNum()).distinct().toList());
            throw new BulkStatusUpdateRejectedException(
                    new BulkStatusRejection(missingIds, occupiedCabinets));
        }

        LocalDateTime now = LocalDateTime.now();
        List<BulkStatusUpdateResponse.UpdatedCabinet> updatedCabinets = new ArrayList<>();
        List<BulkStatusUpdateResponse.EndedLent> endedLents = new ArrayList<>();

        for (Cabinet cabinet : cabinets) {
            if (request.endActiveLents()) {
                for (LentHistory lent :
                        activeLentsByCabinetId.getOrDefault(cabinet.getId(), List.of())) {
                    lent.endLent(now);
                    endedLents.add(
                            new BulkStatusUpdateResponse.EndedLent(
                                    lent.getId(),
                                    cabinet.getId(),
                                    cabinet.getVisibleNum(),
                                    lent.getUser().getId(),
                                    lent.getUser().getName()));
                    log.warn(
                            "[BULK:{}] 대여 종료 - 대여 ID: {}, 사용자: {}(id={}), 사물함: {}",
                            batchId,
                            lent.getId(),
                            lent.getUser().getName(),
                            lent.getUser().getId(),
                            cabinet.getVisibleNum());
                }
            }

            CabinetStatus previousStatus = cabinet.getStatus();
            LentType previousLentType = cabinet.getLentType();
            String previousNote = cabinet.getStatusNote();

            CabinetStatus newStatus = request.status() != null ? request.status() : previousStatus;
            LentType newLentType =
                    request.lentType() != null ? request.lentType() : previousLentType;
            String newNote =
                    (request.statusNote() != null && !request.statusNote().isBlank())
                            ? request.statusNote()
                            : previousNote;
            cabinet.updateStatus(newStatus, newLentType, newNote);

            updatedCabinets.add(
                    new BulkStatusUpdateResponse.UpdatedCabinet(
                            cabinet.getId(), cabinet.getVisibleNum(), previousStatus, newStatus));
            log.info(
                    "[BULK:{}] 사물함 {}(id={}) 변경 - status: {} -> {}, lentType: {} -> {}, 사유: '{}' -> '{}'",
                    batchId,
                    cabinet.getVisibleNum(),
                    cabinet.getId(),
                    previousStatus,
                    newStatus,
                    previousLentType,
                    newLentType,
                    sanitizeForLog(previousNote),
                    sanitizeForLog(newNote));
        }

        log.info(
                "[BULK:{}] 완료 - 관리자: {}, 변경 {}개, 종료된 대여 {}건",
                batchId,
                actor,
                updatedCabinets.size(),
                endedLents.size());
        return new BulkStatusUpdateResponse(batchId, updatedCabinets, endedLents);
    }

    private void validateBulkRequest(BulkStatusUpdateRequest request) {
        List<Long> cabinetIds = request.cabinetIds();
        CabinetStatus targetStatus = request.status();
        String statusNote = request.statusNote();

        if (cabinetIds == null || cabinetIds.isEmpty()) {
            throw new IllegalArgumentException("사물함 ID 목록이 비어있습니다.");
        }

        if (cabinetIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("사물함 ID 목록에 null 이 포함되어 있습니다.");
        }

        if (targetStatus == CabinetStatus.FULL || targetStatus == CabinetStatus.OVERDUE) {
            throw new IllegalArgumentException(
                    "FULL/OVERDUE 는 대여 상태에 따라 자동으로 정해지는 값이라 일괄 변경으로 지정할 수 없습니다.");
        }

        if (targetStatus == CabinetStatus.BROKEN && (statusNote == null || statusNote.isBlank())) {
            throw new IllegalArgumentException("고장 상태로 변경 시 사유(statusNote)는 필수입니다.");
        }

        if (statusNote != null && statusNote.length() > STATUS_NOTE_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "사유(statusNote)는 " + STATUS_NOTE_MAX_LENGTH + "자 이하여야 합니다.");
        }

        if (request.endActiveLents() && !LENT_ENDING_STATUSES.contains(targetStatus)) {
            throw new IllegalArgumentException(
                    "endActiveLents=true 는 status 를 AVAILABLE/BROKEN/DISABLED/PENDING 중 하나로 함께 지정해야 합니다.");
        }
    }

    /** 상태를 바꾸는 요청일 때만 대여를 조회한다. lentType/사유만 바꾸는 요청은 대여를 건드리지 않는다. */
    private Map<Long, List<LentHistory>> loadActiveLents(
            List<Cabinet> cabinets, CabinetStatus targetStatus) {
        if (targetStatus == null || cabinets.isEmpty()) {
            return Map.of();
        }
        List<Long> cabinetIds = cabinets.stream().map(Cabinet::getId).toList();
        return lentRepository.findAllActiveLentByCabinetIds(cabinetIds).stream()
                .collect(Collectors.groupingBy(lent -> lent.getCabinet().getId()));
    }

    private List<BulkStatusRejection.OccupiedCabinet> findOccupiedCabinets(
            List<Cabinet> cabinets, Map<Long, List<LentHistory>> activeLentsByCabinetId) {
        return cabinets.stream()
                .flatMap(
                        cabinet ->
                                activeLentsByCabinetId
                                        .getOrDefault(cabinet.getId(), List.of())
                                        .stream()
                                        .map(
                                                lent ->
                                                        new BulkStatusRejection.OccupiedCabinet(
                                                                cabinet.getId(),
                                                                cabinet.getVisibleNum(),
                                                                lent.getUser().getId(),
                                                                lent.getUser().getName())))
                .toList();
    }

    /** 관리자 입력이 로그에 줄바꿈 등 제어문자를 끼워 넣어 로그 줄을 위조하지 못하게 한다. */
    private static String sanitizeForLog(String value) {
        return value == null ? null : value.replaceAll("\\p{Cntrl}", " ");
    }
}
