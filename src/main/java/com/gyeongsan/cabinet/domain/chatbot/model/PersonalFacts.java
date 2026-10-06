package com.gyeongsan.cabinet.domain.chatbot.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 개인화 답변에 담는 사실의 화이트리스트. 인텐트마다 아래 레코드 하나만 쓰며, 여기에 없는 값(이메일, 코인 내역, 이전 사용자의 반납 메모 등)은 응답에 실을 수 없다.
 * 필드를 늘리면 {@code PersonalFactsContractTest} 가 실패해 검토를 강제한다.
 */
public sealed interface PersonalFacts {

    /** 내 대여 만료 정보. 대여 중이 아니면 나머지는 null. */
    record LentExpiry(
            boolean lentActive,
            Integer visibleNum,
            LocalDateTime expiredAt,
            Integer daysRemaining,
            Boolean overdue)
            implements PersonalFacts {}

    /** 내 패널티. 패널티가 없으면 releaseDate 는 null. */
    record PenaltyStatus(int penaltyDays, LocalDate releaseDate) implements PersonalFacts {}

    /** 대여권 지급 조건과 내 현황. */
    record TicketCondition(
            int thresholdMinutes,
            int monthlyLogtimeMinutes,
            boolean meetsThreshold,
            boolean hasUnusedTicket,
            LocalDate nextPayDate)
            implements PersonalFacts {}

    enum Blocker {
        PENALTY,
        ALREADY_LENDING,
        NO_TICKET
    }

    /** 대여 가능한 사물함 종류(라피신 여부에 따라 갈린다). */
    enum AllowedCabinets {
        LAPISCINE_ONLY,
        EXCEPT_LAPISCINE
    }

    /** 사용자 조건으로 대여가 막히는 사유. 특정 사물함의 상태(남의 예약, 사용 중)는 판단하지 않는다. */
    record LentBlockers(
            List<Blocker> blockers,
            int penaltyDays,
            LocalDate penaltyReleaseDate,
            Integer activeVisibleNum,
            AllowedCabinets allowedCabinets)
            implements PersonalFacts {}
}
