package com.gyeongsan.cabinet.adapter.in.web.admin.dto;

import com.gyeongsan.cabinet.domain.admin.model.UndoConflictCode;
import java.util.List;

/** Undo 가 거부된 이유. 거부는 전체 단위이므로 충돌을 한 번에 모두 담는다. */
public record UndoRejection(String undoneByBatchId, List<Conflict> conflicts) {

    /** cabinetId/visibleNum/userId 는 해당되는 경우에만 채운다. detail 은 사람이 읽는 설명이다. */
    public record Conflict(
            UndoConflictCode code,
            Long cabinetId,
            Integer visibleNum,
            Long userId,
            String detail) {}
}
