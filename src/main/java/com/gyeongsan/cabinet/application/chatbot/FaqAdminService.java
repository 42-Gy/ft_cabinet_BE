package com.gyeongsan.cabinet.application.chatbot;

import com.gyeongsan.cabinet.domain.chatbot.model.Faq;
import com.gyeongsan.cabinet.domain.chatbot.port.in.FaqAdminUseCase;
import com.gyeongsan.cabinet.domain.chatbot.port.in.FaqQueryUseCase;
import com.gyeongsan.cabinet.domain.chatbot.port.out.FaqRepositoryPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.log4j.Log4j2;

/** FAQ 관리(관리자)와 사용자 조회. 저장하면 이 서버의 검색 색인을 바로 다시 만들도록 요청한다(다른 서버는 주기 확인으로 따라온다). */
@Log4j2
public class FaqAdminService implements FaqAdminUseCase, FaqQueryUseCase {

    static final int MAX_CATEGORY_LENGTH = 30;
    static final int MAX_ANSWER_LENGTH = 2000;
    static final int MAX_QUESTION_LENGTH = 200;
    static final int MAX_QUESTIONS_PER_FAQ = 10;

    private final FaqRepositoryPort faqRepository;
    private final FaqIndexManager indexManager;

    public FaqAdminService(FaqRepositoryPort faqRepository, FaqIndexManager indexManager) {
        this.faqRepository = faqRepository;
        this.indexManager = indexManager;
    }

    @Override
    public List<Faq> listAll() {
        return faqRepository.findAll();
    }

    @Override
    public Faq get(long id) {
        return faqRepository
                .findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.FAQ_NOT_FOUND));
    }

    @Override
    public Faq create(String category, String answer, boolean enabled, List<String> questions) {
        Faq faq = validated(null, null, category, answer, enabled, questions, null);
        Faq saved = faqRepository.save(faq);
        log.info("[Chatbot] FAQ 추가 id={}", saved.id());
        indexManager.requestRebuild();
        return saved;
    }

    @Override
    public Faq update(
            long id, String category, String answer, boolean enabled, List<String> questions) {
        Faq existing = get(id);
        Faq faq =
                validated(
                        existing.id(),
                        existing.seedKey(),
                        category,
                        answer,
                        enabled,
                        questions,
                        existing.createdAt());
        Faq saved = faqRepository.save(faq);
        log.info("[Chatbot] FAQ 수정 id={}", saved.id());
        indexManager.requestRebuild();
        return saved;
    }

    @Override
    public void delete(long id) {
        if (!faqRepository.deleteById(id)) {
            throw new ServiceException(ErrorCode.FAQ_NOT_FOUND);
        }
        log.info("[Chatbot] FAQ 삭제 id={}", id);
        indexManager.requestRebuild();
    }

    @Override
    public List<Faq> listEnabled() {
        return faqRepository.findAllEnabled();
    }

    @Override
    public Faq getEnabled(long id) {
        return faqRepository
                .findById(id)
                .filter(Faq::enabled)
                .orElseThrow(() -> new ServiceException(ErrorCode.FAQ_NOT_FOUND));
    }

    static Faq validated(
            Long id,
            String seedKey,
            String category,
            String answer,
            boolean enabled,
            List<String> questions,
            java.time.LocalDateTime createdAt) {
        String cleanCategory = TextNormalizer.normalize(category);
        // 답변은 줄바꿈을 살려야 해서 공백 정리 없이 NFC 와 앞뒤 공백만 정리한다.
        String cleanAnswer =
                answer == null
                        ? ""
                        : java.text.Normalizer.normalize(answer, java.text.Normalizer.Form.NFC)
                                .strip();
        if (cleanCategory.isEmpty() || cleanCategory.length() > MAX_CATEGORY_LENGTH) {
            throw new ServiceException(ErrorCode.FAQ_INVALID);
        }
        if (cleanAnswer.isEmpty() || cleanAnswer.length() > MAX_ANSWER_LENGTH) {
            throw new ServiceException(ErrorCode.FAQ_INVALID);
        }
        Set<String> unique = new LinkedHashSet<>();
        if (questions != null) {
            for (String question : questions) {
                String normalized = TextNormalizer.normalize(question);
                if (normalized.isEmpty() || normalized.length() > MAX_QUESTION_LENGTH) {
                    throw new ServiceException(ErrorCode.FAQ_INVALID);
                }
                unique.add(normalized);
            }
        }
        if (unique.isEmpty() || unique.size() > MAX_QUESTIONS_PER_FAQ) {
            throw new ServiceException(ErrorCode.FAQ_INVALID);
        }
        return new Faq(
                id,
                seedKey,
                cleanCategory,
                cleanAnswer,
                enabled,
                new ArrayList<>(unique),
                createdAt,
                null);
    }
}
