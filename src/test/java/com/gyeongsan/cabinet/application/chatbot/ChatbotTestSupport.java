package com.gyeongsan.cabinet.application.chatbot;

import com.gyeongsan.cabinet.domain.chatbot.model.ChatbotAnswer;
import com.gyeongsan.cabinet.domain.chatbot.model.EmbeddingUnavailableException;
import com.gyeongsan.cabinet.domain.chatbot.model.Faq;
import com.gyeongsan.cabinet.domain.chatbot.port.out.ChatbotMetricsPort;
import com.gyeongsan.cabinet.domain.chatbot.port.out.EmbeddingPort;
import com.gyeongsan.cabinet.domain.chatbot.port.out.FaqRepositoryPort;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/** 챗봇 테스트용 가짜 포트 모음. */
final class ChatbotTestSupport {

    private ChatbotTestSupport() {}

    /** 문장마다 미리 정한 방향의 단위 벡터를 돌려준다. 정해두지 않은 문장은 "아무와도 비슷하지 않은" 벡터. */
    static class FakeEmbedding implements EmbeddingPort {
        final Map<String, float[]> vectors = new HashMap<>();
        final List<String> calls = new ArrayList<>();
        boolean unavailable;
        int dimension = 4;

        FakeEmbedding on(String text, double... components) {
            float[] v = new float[dimension];
            double norm = 0;
            for (double c : components) {
                norm += c * c;
            }
            norm = Math.sqrt(norm);
            for (int i = 0; i < components.length; i++) {
                v[i] = (float) (components[i] / norm);
            }
            vectors.put(text, v);
            return this;
        }

        @Override
        public synchronized float[] embed(String text) {
            if (unavailable) {
                throw new EmbeddingUnavailableException("모델 없음");
            }
            calls.add(text);
            float[] v = vectors.get(text);
            if (v != null) {
                return v.clone();
            }
            // 어떤 FAQ 와도 직교에 가까운 벡터
            float[] other = new float[dimension];
            other[dimension - 1] = 1f;
            return other;
        }

        @Override
        public String modelId() {
            return "fake";
        }
    }

    static class FakeFaqRepository implements FaqRepositoryPort {
        final Map<Long, Faq> store = new java.util.LinkedHashMap<>();
        long nextId = 1;
        int version = 0;
        int fingerprintCalls;

        @Override
        public List<Faq> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public List<Faq> findAllEnabled() {
            return store.values().stream().filter(Faq::enabled).toList();
        }

        @Override
        public Optional<Faq> findById(long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Faq save(Faq faq) {
            long id = faq.id() == null ? nextId++ : faq.id();
            LocalDateTime now = LocalDateTime.of(2026, 10, 7, 12, 0).plusSeconds(++version);
            Faq saved =
                    new Faq(
                            id,
                            faq.seedKey(),
                            faq.category(),
                            faq.answer(),
                            faq.enabled(),
                            faq.questions(),
                            faq.createdAt() == null ? now : faq.createdAt(),
                            now);
            store.put(id, saved);
            return saved;
        }

        @Override
        public boolean deleteById(long id) {
            version++;
            return store.remove(id) != null;
        }

        @Override
        public long count() {
            return store.size();
        }

        @Override
        public String fingerprint() {
            fingerprintCalls++;
            return store.size() + "|" + version;
        }
    }

    static class CountingMetrics implements ChatbotMetricsPort {
        final Map<ChatbotAnswer.Result, AtomicInteger> counts =
                new EnumMap<>(ChatbotAnswer.Result.class);

        @Override
        public void recordAsk(ChatbotAnswer.Result result) {
            counts.computeIfAbsent(result, r -> new AtomicInteger()).incrementAndGet();
        }

        int count(ChatbotAnswer.Result result) {
            return counts.getOrDefault(result, new AtomicInteger()).get();
        }
    }

    static Faq faq(long id, String category, String answer, String... questions) {
        return new Faq(
                id,
                null,
                category,
                answer,
                true,
                List.of(questions),
                LocalDateTime.of(2026, 10, 7, 0, 0),
                LocalDateTime.of(2026, 10, 7, 0, 0));
    }
}
