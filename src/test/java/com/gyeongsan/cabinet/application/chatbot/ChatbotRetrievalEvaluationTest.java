package com.gyeongsan.cabinet.application.chatbot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyeongsan.cabinet.adapter.out.embedding.CharNgramEmbeddingAdapter;
import com.gyeongsan.cabinet.adapter.out.embedding.OnnxEmbeddingAdapter;
import com.gyeongsan.cabinet.domain.chatbot.port.out.EmbeddingPort;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * FAQ 의미 검색의 품질 평가. 초기 FAQ(faq-seed.json)의 질문 표현으로 색인을 만들고, 다르게 표현한 질문(eval-set.json)이 올바른 FAQ 를
 * 찾는지, 범위 밖 질문을 잘 거르는지 임계값별로 측정한다.
 *
 * <ul>
 *   <li>항상 글자 겹침 기준선(ngram)을 측정해 출력한다. 실제 모델이 이 기준선보다 얼마나 나은지 보려는 것이다(기준선은 통과/실패에 영향이 없다).
 *   <li>환경변수 CHATBOT_EVAL_MODELS_DIR 가 있으면 그 아래 하위 디렉터리마다(model.onnx, tokenizer.json,
 *       model.properties) 실제 모델을 평가하고, 기준에 못 미치면 실패한다. GitHub Actions 의 "챗봇 모델 평가"가 모델을 내려받아 이
 *       환경변수를 채워 실행한다.
 *   <li>결과표는 build/chatbot-eval/report.md 에 저장된다(Actions 가 요약과 아티팩트로 올린다).
 * </ul>
 *
 * 기준: CHATBOT_EVAL_MIN_TOP1(기본 0.80), 그리고 "범위 밖 오답 수락률 5% 이하이면서 정밀도 95% 이상, 커버리지 50% 이상"인 임계값이 존재해야
 * 한다.
 */
class ChatbotRetrievalEvaluationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final double[] THRESHOLDS = {
        0.30, 0.35, 0.40, 0.45, 0.50, 0.55, 0.60, 0.65, 0.70, 0.75, 0.80, 0.85, 0.90, 0.95
    };

    record SeedFaq(
            String seedKey,
            String category,
            String answer,
            Boolean enabled,
            List<String> questions) {}

    record EvalItem(String expected, String question) {}

    record EvalSet(List<EvalItem> inScope, List<String> outOfScope) {}

    record Candidate(String id, EmbeddingPort port, boolean required) {}

    record Row(double threshold, double coverage, double precision, double oosFalseAccept) {}

    record Result(
            String id,
            double top1,
            double top3,
            double meanInScope,
            double meanOutOfScope,
            List<Row> rows,
            Row recommended) {}

    private static <T> T read(String resource, TypeReference<T> type) throws IOException {
        try (InputStream in = ChatbotRetrievalEvaluationTest.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("리소스가 없습니다: " + resource);
            }
            return MAPPER.readValue(in, type);
        }
    }

    private static List<SeedFaq> loadFaqs() throws IOException {
        try (InputStream in =
                new java.io.FileInputStream("src/main/resources/chatbot/faq-seed.json")) {
            return MAPPER.readValue(in, new TypeReference<List<SeedFaq>>() {});
        }
    }

    private static List<Candidate> candidates() throws IOException {
        List<Candidate> list = new ArrayList<>();
        list.add(new Candidate("char-ngram(기준선)", new CharNgramEmbeddingAdapter(), false));

        String root = System.getenv("CHATBOT_EVAL_MODELS_DIR");
        if (root != null && !root.isBlank()) {
            try (Stream<Path> dirs = Files.list(Path.of(root))) {
                for (Path dir : dirs.filter(Files::isDirectory).sorted().toList()) {
                    Path model = dir.resolve("model.onnx");
                    Path tokenizer = dir.resolve("tokenizer.json");
                    if (!Files.exists(model) || !Files.exists(tokenizer)) {
                        continue;
                    }
                    Properties props = new Properties();
                    Path propsFile = dir.resolve("model.properties");
                    if (Files.exists(propsFile)) {
                        try (var reader =
                                Files.newBufferedReader(propsFile, StandardCharsets.UTF_8)) {
                            props.load(reader);
                        }
                    }
                    list.add(
                            new Candidate(
                                    dir.getFileName().toString(),
                                    new OnnxEmbeddingAdapter(
                                            dir.getFileName().toString(),
                                            model,
                                            tokenizer,
                                            props.getProperty("prefix", ""),
                                            Integer.parseInt(props.getProperty("maxTokens", "128")),
                                            2),
                                    true));
                }
            }
        }
        return list;
    }

    private static Result evaluate(Candidate candidate, List<SeedFaq> faqs, EvalSet evalSet) {
        EmbeddingPort port = candidate.port();
        List<FaqIndex.Entry> entries = new ArrayList<>();
        Map<Long, com.gyeongsan.cabinet.domain.chatbot.model.Faq> byId = new java.util.HashMap<>();
        Map<Long, String> keyById = new java.util.HashMap<>();
        long id = 1;
        for (SeedFaq faq : faqs) {
            keyById.put(id, faq.seedKey());
            byId.put(
                    id,
                    new com.gyeongsan.cabinet.domain.chatbot.model.Faq(
                            id,
                            faq.seedKey(),
                            faq.category(),
                            faq.answer(),
                            true,
                            faq.questions(),
                            null,
                            null));
            for (String q : faq.questions()) {
                entries.add(new FaqIndex.Entry(id, q, port.embed(TextNormalizer.normalize(q))));
            }
            id++;
        }
        FaqIndex index = new FaqIndex(entries, byId);

        int top1 = 0;
        int top3 = 0;
        double sumIn = 0;
        List<double[]> inScope = new ArrayList<>(); // {최고점, 정답 여부(1/0)}
        for (EvalItem item : evalSet.inScope()) {
            List<FaqIndex.Hit> hits =
                    index.search(port.embed(TextNormalizer.normalize(item.question())), 3);
            boolean first = keyById.get(hits.get(0).faq().id()).equals(item.expected());
            if (first) {
                top1++;
            }
            if (hits.stream().anyMatch(h -> keyById.get(h.faq().id()).equals(item.expected()))) {
                top3++;
            }
            sumIn += hits.get(0).score();
            inScope.add(new double[] {hits.get(0).score(), first ? 1 : 0});
        }
        double sumOut = 0;
        List<Double> outScores = new ArrayList<>();
        for (String question : evalSet.outOfScope()) {
            double best =
                    index.search(port.embed(TextNormalizer.normalize(question)), 1).get(0).score();
            sumOut += best;
            outScores.add(best);
        }

        List<Row> rows = new ArrayList<>();
        for (double t : THRESHOLDS) {
            long accepted = inScope.stream().filter(r -> r[0] >= t).count();
            long correct = inScope.stream().filter(r -> r[0] >= t && r[1] == 1).count();
            long falseAccept = outScores.stream().filter(s -> s >= t).count();
            rows.add(
                    new Row(
                            t,
                            (double) accepted / inScope.size(),
                            accepted == 0 ? 1.0 : (double) correct / accepted,
                            (double) falseAccept / outScores.size()));
        }
        Row recommended =
                rows.stream()
                        .filter(
                                r ->
                                        r.oosFalseAccept() <= 0.05
                                                && r.precision() >= 0.95
                                                && r.coverage() >= 0.50)
                        .min(Comparator.comparingDouble(Row::threshold))
                        .orElse(null);

        return new Result(
                candidate.id(),
                (double) top1 / inScope.size(),
                (double) top3 / inScope.size(),
                sumIn / inScope.size(),
                sumOut / outScores.size(),
                rows,
                recommended);
    }

    private static String format(Result r) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "### %s%n%n", r.id()));
        sb.append(
                String.format(
                        Locale.ROOT,
                        "- 상위 1개 적중률(top-1): **%.1f%%**, 상위 3개 안에 정답(top-3): %.1f%%%n",
                        r.top1() * 100,
                        r.top3() * 100));
        sb.append(
                String.format(
                        Locale.ROOT,
                        "- 평균 최고 유사도: 범위 안 %.3f / 범위 밖 %.3f%n",
                        r.meanInScope(),
                        r.meanOutOfScope()));
        sb.append(
                r.recommended() == null
                        ? "- 권장 match-threshold: **기준을 만족하는 값 없음** (범위 밖 오답 수락 5%↓, 정밀도 95%↑, 커버리지 50%↑)\n\n"
                        : String.format(
                                Locale.ROOT,
                                "- 권장 match-threshold: **%.2f** (정밀도 %.1f%%, 커버리지 %.1f%%, 범위 밖 오답 수락 %.1f%%)%n%n",
                                r.recommended().threshold(),
                                r.recommended().precision() * 100,
                                r.recommended().coverage() * 100,
                                r.recommended().oosFalseAccept() * 100));
        sb.append("| 임계값 | 커버리지(답변한 비율) | 정밀도(답변 중 정답) | 범위 밖 오답 수락 |\n|---|---|---|---|\n");
        for (Row row : r.rows()) {
            sb.append(
                    String.format(
                            Locale.ROOT,
                            "| %.2f | %.1f%% | %.1f%% | %.1f%% |%n",
                            row.threshold(),
                            row.coverage() * 100,
                            row.precision() * 100,
                            row.oosFalseAccept() * 100));
        }
        sb.append('\n');
        return sb.toString();
    }

    @Test
    @DisplayName("FAQ 의미 검색 평가: 재표현 질문을 올바른 FAQ 로 찾고, 범위 밖 질문은 거르는지 임계값별로 측정한다")
    void evaluate() throws Exception {
        List<SeedFaq> faqs = loadFaqs();
        EvalSet evalSet = read("/chatbot/eval-set.json", new TypeReference<EvalSet>() {});
        assertThat(faqs).hasSizeGreaterThanOrEqualTo(20);
        assertThat(evalSet.inScope()).hasSizeGreaterThanOrEqualTo(60);
        assertThat(evalSet.outOfScope()).hasSizeGreaterThanOrEqualTo(15);
        // 평가 질문이 가리키는 FAQ 가 실제로 있어야 한다.
        List<String> keys = faqs.stream().map(SeedFaq::seedKey).toList();
        assertThat(evalSet.inScope()).allSatisfy(i -> assertThat(keys).contains(i.expected()));
        // 평가 질문이 색인된 질문 표현과 글자 그대로 같으면 평가가 아니라 암기다.
        List<String> indexed = faqs.stream().flatMap(f -> f.questions().stream()).toList();
        assertThat(evalSet.inScope())
                .allSatisfy(i -> assertThat(indexed).doesNotContain(i.question()));

        double minTop1 =
                Double.parseDouble(System.getenv().getOrDefault("CHATBOT_EVAL_MIN_TOP1", "0.80"));
        List<Candidate> candidates = candidates();

        StringBuilder report = new StringBuilder("## 챗봇 FAQ 의미 검색 평가\n\n");
        report.append(
                String.format(
                        Locale.ROOT,
                        "FAQ %d건, 평가 질문(범위 안) %d건, 범위 밖 %d건%n%n",
                        faqs.size(),
                        evalSet.inScope().size(),
                        evalSet.outOfScope().size()));
        List<String> failures = new ArrayList<>();
        try {
            for (Candidate candidate : candidates) {
                Result result = evaluate(candidate, faqs, evalSet);
                report.append(format(result));
                if (candidate.required()) {
                    if (result.top1() < minTop1) {
                        failures.add(
                                String.format(
                                        Locale.ROOT,
                                        "%s: top-1 %.1f%% < 기준 %.0f%%",
                                        result.id(),
                                        result.top1() * 100,
                                        minTop1 * 100));
                    }
                    if (result.recommended() == null) {
                        failures.add(result.id() + ": 정밀도/오답 수락 기준을 만족하는 임계값이 없음");
                    }
                }
            }
            if (candidates.size() == 1) {
                report.append("> 실제 모델 후보가 없어 글자 겹침 기준선만 측정했습니다(CHATBOT_EVAL_MODELS_DIR 미설정).\n");
            }
        } finally {
            for (Candidate candidate : candidates) {
                if (candidate.port() instanceof AutoCloseable closeable) {
                    closeable.close();
                }
            }
        }

        Path out = Path.of("build/chatbot-eval/report.md");
        Files.createDirectories(out.getParent());
        Files.writeString(out, report.toString(), StandardCharsets.UTF_8);
        System.out.println(report);

        assertThat(failures).as("평가 기준 미달").isEmpty();
    }
}
