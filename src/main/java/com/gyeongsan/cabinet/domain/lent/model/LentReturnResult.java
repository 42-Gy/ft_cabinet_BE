package com.gyeongsan.cabinet.domain.lent.model;

import java.time.LocalDateTime;

/**
 * 반납 처리 결과. 반납 직전 시점 기준의 만료/연체 정보를 담는다.
 *
 * @param expiredAt 반납한 대여의 만료 시각
 * @param daysRemaining 반납 시점의 남은 일수(달력 기준, 음수면 만료일이 지난 일수)
 * @param overdue 반납 시점에 연체였는지 (패널티 부과와 같은 판정)
 * @param penaltyAppliedDays 이번 반납으로 새로 붙은 패널티 일수. 연체가 아니면 0
 * @param returnedAt 반납 처리 시각
 */
public record LentReturnResult(
        LocalDateTime expiredAt,
        int daysRemaining,
        boolean overdue,
        int penaltyAppliedDays,
        LocalDateTime returnedAt) {}
