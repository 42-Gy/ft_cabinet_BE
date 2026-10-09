package com.gyeongsan.cabinet.adapter.in.web.admin.dto;

import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import java.util.List;

/** 사물함 일괄 변경 결과 요약. batchId 는 서버 로그의 [BULK:batchId] 와 같은 값이다. */
public record BulkStatusUpdateResponse(
        String batchId, List<UpdatedCabinet> updatedCabinets, List<EndedLent> endedLents) {

    public record UpdatedCabinet(
            Long cabinetId,
            Integer visibleNum,
            CabinetStatus previousStatus,
            CabinetStatus status) {}

    public record EndedLent(
            Long lentHistoryId, Long cabinetId, Integer visibleNum, Long userId, String userName) {}
}
