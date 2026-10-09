package com.gyeongsan.cabinet.domain.admin.service;

import com.gyeongsan.cabinet.adapter.in.web.admin.dto.*;
import com.gyeongsan.cabinet.domain.admin.port.in.AdminCabinetUseCase;
import com.gyeongsan.cabinet.domain.cabinet.model.Cabinet;
import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import com.gyeongsan.cabinet.domain.cabinet.model.LentType;
import com.gyeongsan.cabinet.domain.cabinet.port.out.CabinetRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.model.LentHistory;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminCabinetService implements AdminCabinetUseCase {

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

    @Override
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

        List<LentHistory> activeLents = lentRepository.findAllActiveLentByCabinetIds(cabinetIds);
        for (LentHistory lent : activeLents) {
            lent.endLent(LocalDateTime.now());
        }

        for (Cabinet cabinet : cabinets) {
            CabinetStatus newStatus = targetStatus != null ? targetStatus : cabinet.getStatus();
            LentType newLentType =
                    request.lentType() != null ? request.lentType() : cabinet.getLentType();
            String newNote =
                    (statusNote != null && !statusNote.isBlank())
                            ? statusNote
                            : cabinet.getStatusNote();
            cabinet.updateStatus(newStatus, newLentType, newNote);
        }
    }
}
