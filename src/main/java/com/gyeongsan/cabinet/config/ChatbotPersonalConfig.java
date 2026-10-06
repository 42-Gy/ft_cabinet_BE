package com.gyeongsan.cabinet.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyeongsan.cabinet.application.chatbot.PerUserRateLimiter;
import com.gyeongsan.cabinet.application.chatbot.PersonalChatbotService;
import com.gyeongsan.cabinet.application.chatbot.PersonalIntentRouter;
import com.gyeongsan.cabinet.domain.chatbot.model.PersonalIntent;
import com.gyeongsan.cabinet.domain.chatbot.port.out.ChatbotMetricsPort;
import com.gyeongsan.cabinet.domain.chatbot.port.out.EmbeddingPort;
import com.gyeongsan.cabinet.domain.item.port.out.ItemHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.domain.user.model.LentTicketRewardPolicy;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * 챗봇의 "내 정보" 답변. CHATBOT_ENABLED 와 CHATBOT_PERSONAL_ENABLED 가 모두 true 일 때만 켜진다(기본 꺼짐). 질문 표현 목록이
 * 잘못됐으면(모르는 인텐트, 빈 목록) 설정 실수이므로 부팅 시점에 실패한다.
 */
@Configuration
@ConditionalOnProperty(
        name = {"app.chatbot.enabled", "app.chatbot.personal.enabled"},
        havingValue = "true")
public class ChatbotPersonalConfig {

    private static final String CATALOG = "chatbot/personal-intents.json";

    @Bean
    public PersonalIntentRouter personalIntentRouter(EmbeddingPort chatbotEmbeddingPort)
            throws IOException {
        return new PersonalIntentRouter(loadCatalog(), chatbotEmbeddingPort);
    }

    /** 사용자별 분당 한도. 서버 메모리 기준이라 서버가 여러 대면 서버별로 적용된다. */
    @Bean
    public PerUserRateLimiter personalRateLimiter(
            @Value("${app.chatbot.personal.rate-limit-per-minute:10}") int perMinute) {
        return new PerUserRateLimiter(perMinute, 60_000);
    }

    @Bean
    public PersonalChatbotService personalChatbotService(
            UserRepositoryPort userRepository,
            LentRepositoryPort lentRepository,
            ItemHistoryRepositoryPort itemHistoryRepository,
            LentTicketRewardPolicy rewardPolicy,
            PerUserRateLimiter personalRateLimiter,
            ChatbotMetricsPort metrics) {
        return new PersonalChatbotService(
                userRepository,
                lentRepository,
                itemHistoryRepository,
                rewardPolicy,
                personalRateLimiter,
                metrics,
                Clock.systemDefaultZone());
    }

    public static Map<PersonalIntent, List<String>> loadCatalog() throws IOException {
        Map<String, List<String>> raw;
        try (InputStream in = new ClassPathResource(CATALOG).getInputStream()) {
            raw = new ObjectMapper().readValue(in, new TypeReference<>() {});
        }
        Map<PersonalIntent, List<String>> catalog = new EnumMap<>(PersonalIntent.class);
        for (Map.Entry<String, List<String>> e : raw.entrySet()) {
            PersonalIntent intent =
                    PersonalIntent.fromName(e.getKey())
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    CATALOG
                                                            + " 에 알 수 없는 인텐트가 있습니다: "
                                                            + e.getKey()));
            if (e.getValue() == null || e.getValue().isEmpty()) {
                throw new IllegalStateException(CATALOG + " 의 " + intent + " 질문 표현이 비어 있습니다.");
            }
            catalog.put(intent, List.copyOf(e.getValue()));
        }
        for (PersonalIntent intent : PersonalIntent.values()) {
            if (!catalog.containsKey(intent)) {
                throw new IllegalStateException(CATALOG + " 에 " + intent + " 가 없습니다.");
            }
        }
        return catalog;
    }
}
