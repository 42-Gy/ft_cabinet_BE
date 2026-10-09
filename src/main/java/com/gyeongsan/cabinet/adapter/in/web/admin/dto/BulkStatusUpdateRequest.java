package com.gyeongsan.cabinet.adapter.in.web.admin.dto;

import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import com.gyeongsan.cabinet.domain.cabinet.model.LentType;
import java.util.List;

/**
 * 사물함 일괄 변경 요청.
 *
 * <p>{@code endActiveLents} 를 생략하면 false 로 해석되어, 대여 중인 사물함이 포함된 상태 변경은 거부된다. 월말 일괄 반납처럼 대여를 끝내야 하는
 * 작업은 이 값을 true 로 명시해야 한다.
 *
 * <p>{@code reason} 은 관리자가 이 작업을 한 이유로, 감사 로그에 남는다. 사물함에 표시되는 {@code statusNote} 와는 별개다.
 */
public record BulkStatusUpdateRequest(
        List<Long> cabinetIds,
        CabinetStatus status,
        LentType lentType,
        String statusNote,
        boolean endActiveLents,
        String reason) {}
