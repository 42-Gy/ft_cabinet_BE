package com.gyeongsan.cabinet.application.chatbot;

import com.gyeongsan.cabinet.domain.chatbot.model.Faq;
import com.gyeongsan.cabinet.domain.chatbot.port.out.FaqRepositoryPort;
import java.util.List;
import lombok.extern.log4j.Log4j2;

/**
 * 초기 FAQ 를 넣는다. 테이블이 완전히 비어 있을 때만 넣어서, 관리자가 고치거나 지운 내용을 되살리지 않는다. 서버 여러 대가 동시에 처음 기동해도 seed_key 유일
 * 제약 때문에 같은 항목이 두 번 들어가지 않는다(나중 서버의 삽입은 실패하고 무시한다).
 */
@Log4j2
public class FaqSeeder {

    public record SeedItem(
            String seedKey,
            String category,
            String answer,
            Boolean enabled,
            List<String> questions) {}

    private final FaqRepositoryPort faqRepository;

    public FaqSeeder(FaqRepositoryPort faqRepository) {
        this.faqRepository = faqRepository;
    }

    /**
     * @return 새로 넣은 개수
     */
    public int seedIfEmpty(List<SeedItem> items) {
        if (items == null || items.isEmpty() || faqRepository.count() > 0) {
            return 0;
        }
        int inserted = 0;
        for (SeedItem item : items) {
            try {
                Faq faq =
                        FaqAdminService.validated(
                                null,
                                item.seedKey(),
                                item.category(),
                                item.answer(),
                                item.enabled() == null || item.enabled(),
                                item.questions(),
                                null);
                faqRepository.save(faq);
                inserted++;
            } catch (RuntimeException e) {
                // 다른 서버가 먼저 넣었거나(유일 제약) 항목이 잘못된 경우. 나머지는 계속 진행한다.
                log.warn("[Chatbot] 초기 FAQ 건너뜀(seedKey={}): {}", item.seedKey(), e.toString());
            }
        }
        log.info("[Chatbot] 초기 FAQ {}건 추가", inserted);
        return inserted;
    }
}
