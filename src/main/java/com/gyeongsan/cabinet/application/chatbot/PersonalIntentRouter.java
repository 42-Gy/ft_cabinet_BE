package com.gyeongsan.cabinet.application.chatbot;

import com.gyeongsan.cabinet.domain.chatbot.model.EmbeddingUnavailableException;
import com.gyeongsan.cabinet.domain.chatbot.model.PersonalIntent;
import com.gyeongsan.cabinet.domain.chatbot.port.out.EmbeddingPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;

/**
 * 질문이 "내 정보" 질문과 비슷한지 가려내는 작은 색인(인텐트당 질문 표현 몇 개). FAQ 색인과 같은 임베딩 모델을 쓴다.
 *
 * <p>이 라우터는 <b>칩을 제안할지만</b> 정한다. 개인 정보를 읽는 일은 사용자가 칩을 눌러 별도 API 를 호출해야 일어난다. 그래서 오라우팅(예: 다른 사람 정보를
 * 묻는 질문)이 있어도 보이는 것은 본인 정보뿐이고, 틀린 칩은 사용자가 누르지 않으면 그만이다. 색인은 첫 질문이 들어올 때 한 번 만들고(수십 문장), 모델을 쓸 수 없으면
 * 칩 없이 동작한다(FAQ 답변은 영향 없음).
 */
@Log4j2
public class PersonalIntentRouter {

    private record Entry(PersonalIntent intent, float[] vector) {}

    /** 가장 비슷한 인텐트와 점수. */
    public record Hit(PersonalIntent intent, double score) {}

    private final Map<PersonalIntent, List<String>> catalog;
    private final EmbeddingPort embedding;
    private volatile List<Entry> entries;

    public PersonalIntentRouter(
            Map<PersonalIntent, List<String>> catalog, EmbeddingPort embedding) {
        this.catalog = Map.copyOf(catalog);
        this.embedding = embedding;
    }

    /** 가장 비슷한 인텐트. 색인을 만들 수 없으면(모델 없음) 비어 있다. */
    public Optional<Hit> route(float[] query) {
        List<Entry> built = ensureBuilt();
        if (built.isEmpty()) {
            return Optional.empty();
        }
        PersonalIntent best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Entry entry : built) {
            double score = dot(query, entry.vector());
            if (score > bestScore) {
                bestScore = score;
                best = entry.intent();
            }
        }
        return Optional.of(new Hit(best, bestScore));
    }

    private List<Entry> ensureBuilt() {
        List<Entry> current = entries;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (entries != null) {
                return entries;
            }
            try {
                List<Entry> built = new ArrayList<>();
                for (Map.Entry<PersonalIntent, List<String>> e : catalog.entrySet()) {
                    for (String question : e.getValue()) {
                        String normalized = TextNormalizer.normalize(question);
                        if (!normalized.isEmpty()) {
                            built.add(new Entry(e.getKey(), embedding.embed(normalized)));
                        }
                    }
                }
                entries = List.copyOf(built);
                log.info("[Chatbot] 개인화 인텐트 색인 완료: 질문 표현 {}개", built.size());
                return entries;
            } catch (EmbeddingUnavailableException e) {
                // 저장하지 않으므로 다음 질문에서 다시 시도한다(임베딩 어댑터가 자체적으로 재시도 간격을 둔다).
                return List.of();
            }
        }
    }

    private static double dot(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalStateException("벡터 차원이 다릅니다(" + a.length + " != " + b.length + ").");
        }
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += (double) a[i] * b[i];
        }
        return sum;
    }
}
