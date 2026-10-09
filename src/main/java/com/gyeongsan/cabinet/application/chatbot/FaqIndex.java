package com.gyeongsan.cabinet.application.chatbot;

import com.gyeongsan.cabinet.domain.chatbot.model.Faq;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 메모리에 올려 둔 검색 색인. 한번 만들면 바뀌지 않아서 여러 요청이 동시에 읽어도 안전하다. FAQ 가 수백 건 규모라 전부 훑어도 충분히 빠르다. */
public final class FaqIndex {

    public static final FaqIndex EMPTY = new FaqIndex(List.of(), Map.of());

    /** 질문 표현 하나와 그 벡터. */
    public record Entry(long faqId, String question, float[] vector) {}

    /** FAQ 별로 가장 비슷했던 질문 표현과 점수. */
    public record Hit(Faq faq, String matchedQuestion, double score) {}

    private final List<Entry> entries;
    private final Map<Long, Faq> faqById;

    public FaqIndex(List<Entry> entries, Map<Long, Faq> faqById) {
        this.entries = List.copyOf(entries);
        this.faqById = Map.copyOf(faqById);
    }

    public int size() {
        return entries.size();
    }

    /** 점수 높은 순으로 FAQ 단위(같은 FAQ 의 여러 표현 중 최고점)로 돌려준다. */
    public List<Hit> search(float[] query, int limit) {
        Map<Long, Hit> best = new HashMap<>();
        for (Entry entry : entries) {
            double score = dot(query, entry.vector());
            Hit current = best.get(entry.faqId());
            if (current == null || score > current.score()) {
                best.put(
                        entry.faqId(),
                        new Hit(faqById.get(entry.faqId()), entry.question(), score));
            }
        }
        List<Hit> hits = new ArrayList<>(best.values());
        hits.sort(Comparator.comparingDouble(Hit::score).reversed());
        return hits.size() > limit ? List.copyOf(hits.subList(0, limit)) : List.copyOf(hits);
    }

    private static double dot(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalStateException(
                    "벡터 차원이 다릅니다("
                            + a.length
                            + " != "
                            + b.length
                            + "). 모델이 바뀌었다면 색인을 다시 만들어야 합니다.");
        }
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += (double) a[i] * b[i];
        }
        return sum;
    }
}
