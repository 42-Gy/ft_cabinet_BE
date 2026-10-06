package com.gyeongsan.cabinet.adapter.out.persistence.faq;

import com.gyeongsan.cabinet.domain.chatbot.model.Faq;
import com.gyeongsan.cabinet.domain.chatbot.port.out.FaqRepositoryPort;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class FaqPersistenceAdapter implements FaqRepositoryPort {

    private final FaqJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<Faq> findAll() {
        return repository.findAllWithQuestions().stream()
                .map(FaqPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Faq> findAllEnabled() {
        return repository.findAllEnabledWithQuestions().stream()
                .map(FaqPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Faq> findById(long id) {
        return repository.findByIdWithQuestions(id).map(FaqPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional
    public Faq save(Faq faq) {
        // DATETIME(6) 은 마이크로초라 같은 정밀도로 맞춘다.
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);

        FaqEntity entity;
        if (faq.id() == null) {
            entity = FaqEntity.create();
            entity.setSeedKey(faq.seedKey());
            entity.setCreatedAt(now);
        } else {
            entity =
                    repository
                            .findByIdWithQuestions(faq.id())
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "존재하지 않는 FAQ 입니다: " + faq.id()));
        }
        entity.setCategory(faq.category());
        entity.setAnswer(faq.answer());
        entity.setEnabled(faq.enabled());
        entity.setUpdatedAt(now);
        entity.replaceQuestions(faq.questions());

        return toDomain(repository.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public boolean deleteById(long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public String fingerprint() {
        Object[] row = repository.countAndLastUpdated().get(0);
        return row[0] + "|" + row[1];
    }

    private static Faq toDomain(FaqEntity e) {
        List<String> questions =
                e.getQuestions().stream().map(FaqQuestionEntity::getQuestion).toList();
        return new Faq(
                e.getId(),
                e.getSeedKey(),
                e.getCategory(),
                e.getAnswer(),
                e.isEnabled(),
                questions,
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
