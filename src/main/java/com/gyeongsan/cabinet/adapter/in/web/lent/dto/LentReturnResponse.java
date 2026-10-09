package com.gyeongsan.cabinet.adapter.in.web.lent.dto;

import com.gyeongsan.cabinet.domain.lent.model.LentReturnResult;
import java.time.LocalDateTime;
import lombok.Getter;

/** 반납 응답. 기존 {@code message} 필드는 그대로 두고 만료/연체 정보를 덧붙인다. */
@Getter
public class LentReturnResponse {

    private final String message;

    /** 반납한 대여의 만료 시각(ISO-8601). */
    private final LocalDateTime expiredAtIso;

    /** 반납 시점의 남은 일수(달력 기준). 0은 만료일 당일, 음수는 만료일이 지난 일수. */
    private final int daysRemaining;

    /** 반납 시점에 연체였는지. 패널티 부과 판정과 같은 기준이다. */
    private final boolean overdue;

    /** 이번 반납으로 새로 붙은 패널티 일수. */
    private final int penaltyAppliedDays;

    private final LocalDateTime returnedAt;

    private LentReturnResponse(String message, LentReturnResult result) {
        this.message = message;
        this.expiredAtIso = result.expiredAt();
        this.daysRemaining = result.daysRemaining();
        this.overdue = result.overdue();
        this.penaltyAppliedDays = result.penaltyAppliedDays();
        this.returnedAt = result.returnedAt();
    }

    public static LentReturnResponse of(String message, LentReturnResult result) {
        return new LentReturnResponse(message, result);
    }
}
