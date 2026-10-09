package com.gyeongsan.cabinet.domain.chatbot.model;

import java.util.Optional;

/**
 * 챗봇이 "내 정보"로 답할 수 있는 질문의 종류. 목록에 없는 질문은 개인 정보로 답하지 않는다.
 *
 * <p>누구의 정보인지는 이 값으로 정해지지 않는다. 항상 로그인한 본인이다(요청에서 대상 사용자를 받는 경로가 없다).
 */
public enum PersonalIntent {
    LENT_EXPIRY("내 사물함 만료일 확인"),
    PENALTY_STATUS("내 패널티 확인"),
    LENT_TICKET_CONDITION("대여권 지급 조건 확인"),
    LENT_BLOCKER_DIAGNOSIS("대여가 안 되는 이유 확인");

    private final String label;

    PersonalIntent(String label) {
        this.label = label;
    }

    /** 화면에 칩(버튼)으로 보여 줄 문구. */
    public String label() {
        return label;
    }

    public static Optional<PersonalIntent> fromName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        for (PersonalIntent intent : values()) {
            if (intent.name().equals(name.trim())) {
                return Optional.of(intent);
            }
        }
        return Optional.empty();
    }
}
