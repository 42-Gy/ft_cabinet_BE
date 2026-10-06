package com.gyeongsan.cabinet.application.chatbot;

import com.gyeongsan.cabinet.domain.chatbot.model.PersonalFacts;
import com.gyeongsan.cabinet.domain.chatbot.model.PersonalFacts.AllowedCabinets;
import com.gyeongsan.cabinet.domain.chatbot.model.PersonalFacts.Blocker;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 개인화 답변의 고정 문장. 문장은 여기에 쓰여 있고 값만 채운다(생성형 모델을 쓰지 않는다).
 *
 * <p>정책 표현 원칙: 반납 패널티는 만료 <b>시각</b>이 지나면 붙고, 스케줄러의 연체 전환은 만료일 끝까지 유예한다. 이 불일치가 정리되기 전까지는 "만료일까지는
 * 안전" 같은 약속을 하지 않고 사실(만료 시각, 지나면 패널티 대상)만 말한다.
 */
final class PersonalAnswerTemplates {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm", Locale.KOREA);
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.KOREA);

    private PersonalAnswerTemplates() {}

    static String lentExpiry(PersonalFacts.LentExpiry f) {
        if (!f.lentActive()) {
            return "지금 대여 중인 사물함이 없어요.";
        }
        String base = f.visibleNum() + "번 사물함을 대여 중이에요. 만료 시각은 " + fmt(f.expiredAt()) + "이에요.";
        if (Boolean.TRUE.equals(f.overdue()) || f.daysRemaining() < 0) {
            return base + " 만료 시각이 지났어요. 지금 반납하면 패널티가 붙을 수 있으니 가능한 한 빨리 반납해 주세요.";
        }
        int days = f.daysRemaining();
        if (days == 0) {
            return base + " 오늘이 만료일이에요. 만료 시각이 지난 뒤에 반납하면 패널티 대상이 돼요.";
        }
        return base + " 오늘 기준 " + days + "일 남았어요.";
    }

    static String penalty(PersonalFacts.PenaltyStatus f) {
        if (f.penaltyDays() <= 0) {
            return "지금 적용 중인 패널티가 없어요.";
        }
        return "패널티가 "
                + f.penaltyDays()
                + "일 남아 있어요. 매일 자정에 하루씩 줄어들어 약 "
                + fmt(f.releaseDate())
                + "부터 새로 대여할 수 있어요. 패널티가 남아 있는 동안은 새 사물함 대여가 막혀요.";
    }

    static String ticketCondition(PersonalFacts.TicketCondition f) {
        StringBuilder sb = new StringBuilder();
        sb.append("대여권은 매달 1일 새벽에, 지난달 로그타임이 기준 이상이면 1장 지급돼요. ")
                .append("내 기준은 ")
                .append(minutes(f.thresholdMinutes()))
                .append("이고, 이번 달 집계된 내 로그타임은 ")
                .append(minutes(f.monthlyLogtimeMinutes()))
                .append("이에요(어제까지 집계, 매일 새벽 갱신). ");
        if (f.meetsThreshold()) {
            sb.append("지금 기준으로는 조건을 채웠어요. ");
        } else {
            sb.append("아직 ")
                    .append(minutes(f.thresholdMinutes() - f.monthlyLogtimeMinutes()))
                    .append(" 더 필요해요. ");
        }
        sb.append("다음 지급일은 ").append(fmt(f.nextPayDate())).append("이에요.");
        if (f.hasUnusedTicket()) {
            sb.append(" 지금 쓰지 않은 대여권을 가지고 있어서, 이미 있는 동안에는 새로 지급되지 않아요.");
        }
        return sb.toString();
    }

    static String blockers(PersonalFacts.LentBlockers f) {
        List<String> lines = new ArrayList<>();
        for (Blocker blocker : f.blockers()) {
            switch (blocker) {
                case PENALTY ->
                        lines.add(
                                "패널티가 "
                                        + f.penaltyDays()
                                        + "일 남아 있어요(약 "
                                        + fmt(f.penaltyReleaseDate())
                                        + "부터 대여 가능).");
                case ALREADY_LENDING ->
                        lines.add(
                                "이미 대여 중인 사물함이 있어요("
                                        + f.activeVisibleNum()
                                        + "번). 한 번에 하나만 빌릴 수 있어서 먼저 반납해야 해요.");
                case NO_TICKET ->
                        lines.add("사용하지 않은 대여권이 없어요. 대여권은 상점에서 살 수 없고, 매달 로그타임 기준을 채우면 지급돼요.");
            }
        }
        String scope =
                f.allowedCabinets() == AllowedCabinets.LAPISCINE_ONLY
                        ? "참고: 라피신 참가자는 라피신 전용 사물함만 대여할 수 있어요."
                        : "참고: 라피신 전용 사물함은 대여할 수 없어요.";
        if (lines.isEmpty()) {
            return "내 계정 조건으로는 대여를 막는 사유가 없어요. 그래도 대여가 안 된다면 고른 사물함이 이미 사용 중이거나 "
                    + "다른 사람이 예약한 상태일 수 있어요(번호별 상태는 사물함 목록에서 확인해 주세요). "
                    + scope;
        }
        StringBuilder sb = new StringBuilder("내 계정에서 대여를 막는 사유가 " + lines.size() + "가지 있어요.");
        for (String line : lines) {
            sb.append("\n• ").append(line);
        }
        sb.append("\n").append(scope);
        return sb.toString();
    }

    private static String fmt(LocalDateTime t) {
        return t.format(DATE_TIME);
    }

    private static String fmt(LocalDate d) {
        return d.format(DATE);
    }

    /** 90분 -> "1시간 30분(90분)", 45분 -> "45분". */
    static String minutes(int total) {
        int m = Math.max(total, 0);
        if (m < 60) {
            return m + "분";
        }
        int h = m / 60;
        int rest = m % 60;
        return rest == 0 ? h + "시간(" + m + "분)" : h + "시간 " + rest + "분(" + m + "분)";
    }
}
