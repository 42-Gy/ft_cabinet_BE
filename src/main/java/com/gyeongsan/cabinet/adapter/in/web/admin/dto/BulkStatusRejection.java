package com.gyeongsan.cabinet.adapter.in.web.admin.dto;

import java.util.List;

/** 사물함 일괄 변경이 거부된 이유. 거부는 전체 단위이므로 문제가 된 사물함을 한 번에 모두 담는다. */
public record BulkStatusRejection(
        List<Long> missingCabinetIds, List<OccupiedCabinet> occupiedCabinets) {

    /** endActiveLents 없이 상태를 바꾸려 해서 거부된, 대여 중인 사물함. */
    public record OccupiedCabinet(
            Long cabinetId, Integer visibleNum, Long userId, String userName) {}
}
