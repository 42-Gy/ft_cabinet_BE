package com.gyeongsan.cabinet.domain.lent.model;

public record ReservationOutcome(Status status, Integer replacedVisibleNum) {

    public enum Status {
        RESERVED,
        ALREADY_MINE,
        TAKEN_BY_OTHER
    }
}
