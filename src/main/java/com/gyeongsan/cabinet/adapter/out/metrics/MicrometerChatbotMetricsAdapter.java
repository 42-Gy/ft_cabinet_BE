package com.gyeongsan.cabinet.adapter.out.metrics;

import com.gyeongsan.cabinet.domain.chatbot.model.ChatbotAnswer;
import com.gyeongsan.cabinet.domain.chatbot.model.PersonalIntent;
import com.gyeongsan.cabinet.domain.chatbot.port.out.ChatbotMetricsPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;

/** 결과별 질문 건수를 Prometheus 지표(chatbot_ask_total{result=...})로 노출한다. 질문 내용은 어디에도 남기지 않는다. */
@RequiredArgsConstructor
public class MicrometerChatbotMetricsAdapter implements ChatbotMetricsPort {

    private final MeterRegistry registry;

    @Override
    public void recordAskDuration(long nanos) {
        Timer.builder("chatbot.ask.duration")
                .description("챗봇 질문 처리 시간(임베딩 + 검색)")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry)
                .record(nanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordAsk(ChatbotAnswer.Result result) {
        registry.counter("chatbot.ask", "result", result.name().toLowerCase()).increment();
    }

    @Override
    public void recordPersonal(PersonalIntent intent, String outcome) {
        registry.counter(
                        "chatbot.personal",
                        "intent",
                        intent.name().toLowerCase(),
                        "outcome",
                        outcome)
                .increment();
    }
}
