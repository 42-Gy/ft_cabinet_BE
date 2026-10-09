package com.gyeongsan.cabinet.domain.lent.model;

/**
 * 사물함 예약 시도의 결과.
 *
 * <p>replacedVisibleNum 은 새 예약을 위해 자동 취소된 기존 예약의 사물함 번호이다(없으면 null).
 */
public record ReservationOutcome(Status status, Integer replacedVisibleNum) {

    public enum Status {
        /** 새로 예약되었다. 기존 예약이 있었다면 함께 취소되었다. */
        RESERVED,
        /** 이미 내가 같은 사물함을 예약하고 있다. 아무것도 바뀌지 않았다. */
        ALREADY_MINE,
        /** 다른 사용자가 이 사물함을 예약하고 있다. 아무것도 바뀌지 않았다. */
        TAKEN_BY_OTHER
    }
}
