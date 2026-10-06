package com.gyeongsan.cabinet.domain.chatbot.port.out;

import com.gyeongsan.cabinet.domain.chatbot.model.ChatbotAnswer;
import com.gyeongsan.cabinet.domain.chatbot.model.PersonalIntent;

/** 질문 원문은 기록하지 않고 결과별 건수만 센다. */
public interface ChatbotMetricsPort {

    void recordAsk(ChatbotAnswer.Result result);

    /** 질문 하나를 처리하는 데 걸린 시간(임베딩 + 검색). 지연 분포(p50/p95/p99)를 보려는 것이며 질문 내용은 담지 않는다. */
    default void recordAskDuration(long nanos) {}

    /**
     * 개인화 기능 사용 건수. outcome 은 chip_shown(질문에 칩을 붙임), answered, rate_limited 처럼 값이 정해진 문자열이다. 사용자
     * 식별자나 응답 값은 담지 않는다.
     */
    default void recordPersonal(PersonalIntent intent, String outcome) {}
}
