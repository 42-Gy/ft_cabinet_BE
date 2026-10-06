package com.gyeongsan.cabinet.domain.chatbot.port.out;

import com.gyeongsan.cabinet.domain.chatbot.model.ChatbotAnswer;

/** 질문 원문은 기록하지 않고 결과별 건수만 센다. */
public interface ChatbotMetricsPort {

    void recordAsk(ChatbotAnswer.Result result);
}
