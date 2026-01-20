package com.gyeongsan.cabinet.admin.dto;

public record PenaltyRequest(
        Integer penaltyDays,
        String reason) {
}
