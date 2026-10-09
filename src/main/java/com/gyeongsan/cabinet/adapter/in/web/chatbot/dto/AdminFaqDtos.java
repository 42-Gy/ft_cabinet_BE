package com.gyeongsan.cabinet.adapter.in.web.chatbot.dto;

import com.gyeongsan.cabinet.domain.chatbot.model.Faq;
import java.time.LocalDateTime;
import java.util.List;

public final class AdminFaqDtos {

    private AdminFaqDtos() {}

    /** 모든 검증(길이, 개수, 공백)은 서비스가 한다. enabled 를 생략하면 true 로 본다. */
    public record FaqRequest(
            String category, String answer, Boolean enabled, List<String> questions) {

        boolean enabledOrDefault() {
            return enabled == null || enabled;
        }

        public boolean resolvedEnabled() {
            return enabledOrDefault();
        }
    }

    public record FaqResponse(
            Long id,
            String seedKey,
            String category,
            String answer,
            boolean enabled,
            List<String> questions,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {

        public static FaqResponse from(Faq faq) {
            return new FaqResponse(
                    faq.id(),
                    faq.seedKey(),
                    faq.category(),
                    faq.answer(),
                    faq.enabled(),
                    faq.questions(),
                    faq.createdAt(),
                    faq.updatedAt());
        }
    }
}
