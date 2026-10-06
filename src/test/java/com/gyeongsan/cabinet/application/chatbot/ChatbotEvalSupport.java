package com.gyeongsan.cabinet.application.chatbot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyeongsan.cabinet.domain.chatbot.model.Faq;
import com.gyeongsan.cabinet.domain.chatbot.port.out.EmbeddingPort;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 챗봇 검색 평가용 자료구조와 계산. 보고서 서식은 ChatbotRetrievalEvaluationTest 가 맡는다. */
final class ChatbotEvalSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ChatbotEvalSupport() {}

    record SeedFaq(
            String seedKey,
            String category,
            String answer,
            Boolean enabled,
            List<String> questions) {}

    /** alsoAccept: 질문이 둘 이상의 FAQ 에 걸쳐 있어 정답을 하나로 못 박기 어려울 때, 함께 정답으로 인정할 FAQ. */
    record EvalItem(String expected, String question, List<String> alsoAccept) {
        Set<String> accepted() {
            Set<String> set = new HashSet<>();
            set.add(expected);
            if (alsoAccept != null) {
                set.addAll(alsoAccept);
            }
            return set;
        }
    }

    record EvalSet(List<EvalItem> inScope, List<String> outOfScope) {}

    static <T> T readResource(String resource, TypeReference<T> type) throws IOException {
        try (InputStream in = ChatbotEvalSupport.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("리소스가 없습니다: " + resource);
            }
            return MAPPER.readValue(in, type);
        }
    }

    static List<SeedFaq> loadFaqs() throws IOException {
        try (InputStream in =
                new java.io.FileInputStream("src/main/resources/chatbot/faq-seed.json")) {
            return MAPPER.readValue(in, new TypeReference<List<SeedFaq>>() {});
        }
    }

    /** 같은 문장은 한 번만 임베딩한다(학습곡선처럼 같은 질문을 여러 번 검색할 때). */
    static final class CachingEmbedder {
        private final EmbeddingPort port;
        private final Map<String, float[]> cache = new HashMap<>();

        CachingEmbedder(EmbeddingPort port) {
            this.port = port;
        }

        float[] embed(String text) {
            return cache.computeIfAbsent(TextNormalizer.normalize(text), port::embed);
        }
    }

    /** 질문 하나를 검색한 결과(FAQ 단위 순위). */
    record Probe(
            EvalItem item,
            List<String> rankedKeys,
            List<Double> rankedScores,
            List<String> matchedQuestions) {
        String predicted() {
            return rankedKeys.get(0);
        }

        boolean top1() {
            return item.accepted().contains(predicted());
        }

        /** 정답이 몇 번째에 있는지(1부터). 없으면 0. */
        int rank() {
            for (int i = 0; i < rankedKeys.size(); i++) {
                if (item.accepted().contains(rankedKeys.get(i))) {
                    return i + 1;
                }
            }
            return 0;
        }

        boolean top3() {
            int rank = rank();
            return rank >= 1 && rank <= 3;
        }

        double s1() {
            return rankedScores.get(0);
        }

        double s2() {
            return rankedScores.size() > 1 ? rankedScores.get(1) : 0;
        }

        double margin() {
            return s1() - s2();
        }

        /** 제안 모드: 임계값 이상인 상위 3개 후보 중에 정답이 있는가. */
        boolean suggestionHit(double threshold) {
            for (int i = 0; i < Math.min(3, rankedKeys.size()); i++) {
                if (rankedScores.get(i) >= threshold
                        && item.accepted().contains(rankedKeys.get(i))) {
                    return true;
                }
            }
            return false;
        }
    }

    record OosProbe(String question, double s1, double s2) {
        double margin() {
            return s1 - s2;
        }
    }

    record SetResult(Map<String, String> categoryByKey, List<Probe> in, List<OosProbe> out) {

        double top1() {
            return fraction(in.stream().filter(Probe::top1).count(), in.size());
        }

        double top3() {
            return fraction(in.stream().filter(Probe::top3).count(), in.size());
        }

        /** 예측한 FAQ 가 정답 FAQ 와 같은 카테고리인 비율(정답으로 인정된 FAQ 들의 카테고리 포함). */
        double categoryTop1() {
            long hit =
                    in.stream()
                            .filter(
                                    p ->
                                            p.item().accepted().stream()
                                                    .map(categoryByKey::get)
                                                    .anyMatch(
                                                            c ->
                                                                    c != null
                                                                            && c.equals(
                                                                                    categoryByKey
                                                                                            .get(
                                                                                                    p
                                                                                                            .predicted()))))
                            .count();
            return fraction(hit, in.size());
        }

        double meanIn() {
            return in.stream().mapToDouble(Probe::s1).average().orElse(Double.NaN);
        }

        double meanOut() {
            return out.stream().mapToDouble(OosProbe::s1).average().orElse(Double.NaN);
        }
    }

    static double fraction(long part, long whole) {
        return whole == 0 ? Double.NaN : (double) part / whole;
    }

    /** 색인을 만들어(선택한 변형만) 평가 질문 전체를 검색한다. */
    static SetResult probe(
            List<SeedFaq> faqs,
            List<List<Integer>> selection,
            CachingEmbedder embedder,
            EvalSet evalSet) {
        List<FaqIndex.Entry> entries = new ArrayList<>();
        Map<Long, Faq> byId = new HashMap<>();
        Map<Long, String> keyById = new HashMap<>();
        Map<String, String> categoryByKey = new LinkedHashMap<>();
        for (int i = 0; i < faqs.size(); i++) {
            SeedFaq faq = faqs.get(i);
            long id = i + 1L;
            keyById.put(id, faq.seedKey());
            categoryByKey.put(faq.seedKey(), faq.category());
            byId.put(
                    id,
                    new Faq(
                            id,
                            faq.seedKey(),
                            faq.category(),
                            faq.answer(),
                            true,
                            faq.questions(),
                            null,
                            null));
            for (int variant : selection.get(i)) {
                String q = faq.questions().get(variant);
                entries.add(new FaqIndex.Entry(id, q, embedder.embed(q)));
            }
        }
        FaqIndex index = new FaqIndex(entries, byId);

        List<Probe> in = new ArrayList<>();
        for (EvalItem item : evalSet.inScope()) {
            List<FaqIndex.Hit> hits = index.search(embedder.embed(item.question()), faqs.size());
            in.add(
                    new Probe(
                            item,
                            hits.stream().map(h -> keyById.get(h.faq().id())).toList(),
                            hits.stream().map(FaqIndex.Hit::score).toList(),
                            hits.stream().map(FaqIndex.Hit::matchedQuestion).toList()));
        }
        List<OosProbe> out = new ArrayList<>();
        for (String question : evalSet.outOfScope()) {
            List<FaqIndex.Hit> hits = index.search(embedder.embed(question), 2);
            out.add(
                    new OosProbe(
                            question,
                            hits.get(0).score(),
                            hits.size() > 1 ? hits.get(1).score() : 0));
        }
        return new SetResult(categoryByKey, in, out);
    }

    static List<List<Integer>> allVariants(List<SeedFaq> faqs) {
        List<List<Integer>> selection = new ArrayList<>();
        for (SeedFaq faq : faqs) {
            selection.add(
                    java.util.stream.IntStream.range(0, faq.questions().size()).boxed().toList());
        }
        return selection;
    }

    /** FAQ 마다 변형 k 개만 쓴다. offset 으로 어느 변형부터 쓸지 돌려 가며 평균을 낼 수 있게 한다. */
    static List<List<Integer>> firstK(List<SeedFaq> faqs, int k, int offset) {
        List<List<Integer>> selection = new ArrayList<>();
        for (SeedFaq faq : faqs) {
            int n = faq.questions().size();
            int m = Math.min(k, n);
            List<Integer> picked = new ArrayList<>();
            for (int j = 0; j < m; j++) {
                picked.add((offset + j) % n);
            }
            selection.add(picked);
        }
        return selection;
    }

    // ---- 임계값 표 ----

    /** 제안 모드(임계값 이상인 후보만 보여 줌)와 자동 답변 모드를 함께 보기 위한 한 줄. */
    record Row(
            double threshold,
            double coverage,
            double top1Precision,
            double top3Precision,
            double recall,
            double oosAccept) {}

    static Row row(SetResult result, double threshold) {
        List<Probe> answered = result.in().stream().filter(p -> p.s1() >= threshold).toList();
        long correct1 = answered.stream().filter(Probe::top1).count();
        long correct3 = answered.stream().filter(p -> p.suggestionHit(threshold)).count();
        long recall = result.in().stream().filter(p -> p.suggestionHit(threshold)).count();
        long oos = result.out().stream().filter(o -> o.s1() >= threshold).count();
        return new Row(
                threshold,
                fraction(answered.size(), result.in().size()),
                fraction(correct1, answered.size()),
                fraction(correct3, answered.size()),
                fraction(recall, result.in().size()),
                fraction(oos, result.out().size()));
    }

    static List<Double> coarseGrid() {
        List<Double> grid = new ArrayList<>();
        for (int h = 30; h <= 95; h += 5) {
            grid.add(h / 100.0);
        }
        return grid;
    }

    /** 점수가 몰려 있는 구간을 0.01 단위로 보여 준다(e5 처럼 점수가 좁게 몰리는 모델은 0.05 단위로는 안 보인다). */
    static List<Double> fineGrid(SetResult result) {
        double[] in = result.in().stream().mapToDouble(Probe::s1).sorted().toArray();
        double maxOos = result.out().stream().mapToDouble(OosProbe::s1).max().orElse(0);
        if (in.length == 0) {
            return List.of();
        }
        int lo = (int) Math.floor(Math.max(0.30, quantile(in, 0.05) - 0.02) * 100);
        int hi = (int) Math.ceil(Math.min(0.99, Math.max(quantile(in, 0.95), maxOos) + 0.01) * 100);
        int step = (hi - lo) > 35 ? 2 : 1;
        List<Double> grid = new ArrayList<>();
        for (int h = lo; h <= hi; h += step) {
            grid.add(h / 100.0);
        }
        return grid;
    }

    static List<Row> rows(SetResult result, List<Double> grid) {
        return grid.stream().map(t -> row(result, t)).toList();
    }

    /** 자동 답변(MATCHED) 기준: 범위 밖 수락 5% 이하, 1위 정답률 95% 이상, 커버리지 50% 이상인 가장 낮은 임계값. */
    static Row recommended(SetResult result) {
        List<Double> grid = new ArrayList<>(coarseGrid());
        grid.addAll(fineGrid(result));
        return grid.stream()
                .distinct()
                .sorted()
                .map(t -> row(result, t))
                .filter(
                        r ->
                                r.oosAccept() <= 0.05
                                        && r.top1Precision() >= 0.95
                                        && r.coverage() >= 0.50)
                .min(Comparator.comparingDouble(Row::threshold))
                .orElse(null);
    }

    // ---- 신호 품질 ----

    static double quantile(double[] sorted, double q) {
        if (sorted.length == 0) {
            return Double.NaN;
        }
        double pos = q * (sorted.length - 1);
        int lower = (int) Math.floor(pos);
        int upper = (int) Math.ceil(pos);
        return sorted[lower] + (sorted[upper] - sorted[lower]) * (pos - lower);
    }

    /** 양성 점수가 음성 점수보다 클 확률(0.5 = 구분 못 함, 1.0 = 완벽). 한쪽이 비면 NaN. */
    static double auroc(List<Double> positive, List<Double> negative) {
        if (positive.isEmpty() || negative.isEmpty()) {
            return Double.NaN;
        }
        double wins = 0;
        for (double p : positive) {
            for (double n : negative) {
                wins += p > n ? 1 : (p == n ? 0.5 : 0);
            }
        }
        return wins / ((double) positive.size() * negative.size());
    }

    static double mean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }

    static List<Double> scores(List<Probe> probes, java.util.function.ToDoubleFunction<Probe> f) {
        return probes.stream().map(p -> f.applyAsDouble(p)).toList();
    }

    /** 자동 답변 규칙 "1위 점수 ≥ T 그리고 1·2위 차이 ≥ M" 의 한 칸. */
    record RuleCell(double coverage, double precision, double oosAccept) {}

    static RuleCell rule(SetResult result, double t, double m) {
        List<Probe> passed =
                result.in().stream().filter(p -> p.s1() >= t && p.margin() >= m).toList();
        long correct = passed.stream().filter(Probe::top1).count();
        long oos = result.out().stream().filter(o -> o.s1() >= t && o.margin() >= m).count();
        return new RuleCell(
                fraction(passed.size(), result.in().size()),
                fraction(correct, passed.size()),
                fraction(oos, result.out().size()));
    }

    /** 범위 밖 질문의 최고점 분포에서 뽑은 임계값(범위 밖을 50/75/90/100% 걸러 내는 지점). */
    static List<Double> oosQuantileThresholds(SetResult result) {
        double[] out = result.out().stream().mapToDouble(OosProbe::s1).sorted().toArray();
        if (out.length == 0) {
            return List.of();
        }
        return Arrays.stream(new double[] {0.50, 0.75, 0.90, 1.0})
                .map(q -> Math.min(1.0, Math.ceil(quantile(out, q) * 100) / 100.0))
                .boxed()
                .distinct()
                .toList();
    }

    /** 틀린 질문의 (기대 → 예측) 쌍을 많이 나온 순으로 센다. */
    static List<Map.Entry<String, Integer>> confusions(SetResult result) {
        Map<String, Integer> counts = new HashMap<>();
        for (Probe p : result.in()) {
            if (!p.top1()) {
                counts.merge(p.item().expected() + " → " + p.predicted(), 1, Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .sorted(
                        Map.Entry.<String, Integer>comparingByValue()
                                .reversed()
                                .thenComparing(Map.Entry.comparingByKey()))
                .toList();
    }
}
