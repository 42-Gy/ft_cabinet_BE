package com.gyeongsan.cabinet.adapter.in.web.admin.dto;

import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import java.util.List;

/** Undo 결과 요약. undoBatchId 는 이 Undo 자체의 감사 기록 batchId 이다. */
public record UndoResponse(
        String undoBatchId,
        String originalBatchId,
        List<RestoredCabinet> restoredCabinets,
        List<ReopenedLent> reopenedLents) {

    public record RestoredCabinet(
            Long cabinetId, Integer visibleNum, CabinetStatus fromStatus, CabinetStatus toStatus) {}

    public record ReopenedLent(
            Long lentHistoryId, Long cabinetId, Integer visibleNum, Long userId, String userName) {}
}
