package com.gyeongsan.cabinet.adapter.in.scheduler.chatbot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyeongsan.cabinet.application.chatbot.FaqIndexManager;
import com.gyeongsan.cabinet.application.chatbot.FaqSeeder;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 서버가 뜨면 초기 FAQ 를 넣고(테이블이 비어 있을 때만) 검색 색인을 만든다. 이후 주기적으로 DB 의 FAQ 가 바뀌었는지 확인해 이 서버의 색인을 갱신한다. 색인은
 * 서버마다 따로 갖기 때문에 ShedLock 을 쓰지 않는다.
 */
@Component
@ConditionalOnProperty(name = "app.chatbot.enabled", havingValue = "true")
@RequiredArgsConstructor
@Log4j2
public class ChatbotLifecycle {

    static final String SEED_RESOURCE = "chatbot/faq-seed.json";

    private final FaqSeeder seeder;
    private final FaqIndexManager indexManager;
    private final ObjectMapper objectMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        try {
            seeder.seedIfEmpty(loadSeed());
        } catch (RuntimeException | IOException e) {
            log.error("[Chatbot] 초기 FAQ 를 넣지 못했습니다(색인은 계속 만듭니다): {}", e.toString());
        }
        indexManager.requestRebuild();
    }

    @Scheduled(
            initialDelayString = "${app.chatbot.reload-interval-ms:30000}",
            fixedDelayString = "${app.chatbot.reload-interval-ms:30000}")
    public void refresh() {
        indexManager.refreshIfChanged();
    }

    List<FaqSeeder.SeedItem> loadSeed() throws IOException {
        ClassPathResource resource = new ClassPathResource(SEED_RESOURCE);
        if (!resource.exists()) {
            return List.of();
        }
        try (InputStream in = resource.getInputStream()) {
            return objectMapper.readValue(in, new TypeReference<List<FaqSeeder.SeedItem>>() {});
        }
    }
}
