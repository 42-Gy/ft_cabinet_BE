package com.gyeongsan.cabinet.application.chatbot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyeongsan.cabinet.adapter.out.embedding.OnnxEmbeddingAdapter;
import com.gyeongsan.cabinet.domain.chatbot.port.out.EmbeddingPort;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 실제 모델에서 Java 임베딩 구현(ONNX Runtime + DJL 토크나이저 + 평균 풀링)이 올바른지 확인한다.
 *
 * <p>평가에서 의미 모델이 글자 겹침 기준선보다 나쁘게 나오면 "모델이 약한 것"인지 "구현이 틀린 것"인지 구분할 수 없다. 그래서 같은 모델 저장소의 원본 가중치를
 * sentence-transformers 로 돌린 정답 벡터(scripts/chatbot/golden_vectors.py 가 만든 golden.json)와 Java 어댑터의
 * 결과를 같은 문장에 대해 코사인 유사도로 비교한다. 1.0 에 가까우면 구현은 맞고 품질은 모델의 한계다.
 *
 * <p>환경변수 CHATBOT_EVAL_MODELS_DIR 아래 모델 디렉터리마다 golden.json 이 있어야 실행된다(없으면 건너뜀). 기준은
 * CHATBOT_GOLDEN_MIN_COSINE(기본 0.99, 양자화 모델은 fp32 와 조금 다르다). CHATBOT_EVAL_ENFORCE=false 면 기준 미달이어도
 * 실패시키지 않고 리포트(build/chatbot-eval/golden.md)에만 남긴다.
 */
class ChatbotGoldenVectorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Path TINY = Path.of("src/test/resources/fixtures/chatbot/tiny-model");

    record Comparison(String text, double cosine) {}

    record Summary(String id, String reference, List<Comparison> rows) {
        double min() {
            return rows.stream().mapToDouble(Comparison::cosine).min().orElse(0);
        }

        double mean() {
            return rows.stream().mapToDouble(Comparison::cosine).average().orElse(0);
        }

        String verdict() {
            double min = min();
            if (min >= 0.999) {
                return "일치(구현 정상)";
            }
            if (min >= 0.99) {
                return "근사 일치(양자화 모델이면 정상)";
            }
            return "불일치 — 토크나이저/풀링/입력 처리 점검 필요";
        }
    }

    static double cosine(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalStateException("차원이 다릅니다: " + a.length + " != " + b.length);
        }
        double dot = 0;
        double na = 0;
        double nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            na += (double) a[i] * a[i];
            nb += (double) b[i] * b[i];
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    static Summary compare(
            String id,
            String reference,
            List<String> texts,
            float[][] expected,
            EmbeddingPort port) {
        List<Comparison> rows = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            rows.add(new Comparison(texts.get(i), cosine(port.embed(texts.get(i)), expected[i])));
        }
        return new Summary(id, reference, rows);
    }

    static String format(Summary s) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "### %s%n%n", s.id()));
        sb.append(String.format(Locale.ROOT, "- 참조 구현: %s%n", s.reference()));
        sb.append(
                String.format(
                        Locale.ROOT,
                        "- 판정: **%s** (최소 코사인 %.6f, 평균 %.6f, 문장 %d개)%n%n",
                        s.verdict(),
                        s.min(),
                        s.mean(),
                        s.rows().size()));
        sb.append("<details><summary>문장별 코사인(낮은 순)</summary>\n\n| 코사인 | 문장 |\n|---|---|\n");
        s.rows().stream()
                .sorted(Comparator.comparingDouble(Comparison::cosine))
                .forEach(
                        r -> {
                            String text = r.text().replace("|", "\\|").replace("\n", " ");
                            if (text.length() > 60) {
                                text = text.substring(0, 60) + "…";
                            }
                            sb.append(
                                    String.format(
                                            Locale.ROOT, "| %.6f | %s |%n", r.cosine(), text));
                        });
        sb.append("\n</details>\n\n");
        return sb.toString();
    }

    private static float[][] vectors(JsonNode node) {
        float[][] out = new float[node.size()][];
        for (int i = 0; i < node.size(); i++) {
            JsonNode row = node.get(i);
            out[i] = new float[row.size()];
            for (int j = 0; j < row.size(); j++) {
                out[i][j] = (float) row.get(j).asDouble();
            }
        }
        return out;
    }

    @Test
    @DisplayName("비교 로직 자체 검증: 장난감 모델은 파이썬 기대 벡터와 일치로, 일부러 틀린 기대 벡터는 불일치로 판정한다")
    void comparisonLogicDetectsMismatch() throws IOException {
        JsonNode expected =
                MAPPER.readTree(Files.readString(TINY.resolve("expected.json")))
                        .get("vectors")
                        .get("model");
        List<String> texts = new ArrayList<>();
        List<float[]> good = new ArrayList<>();
        List<float[]> bad = new ArrayList<>();
        expected.fields()
                .forEachRemaining(
                        e -> {
                            texts.add(e.getKey());
                            float[] v = new float[e.getValue().size()];
                            float[] w = new float[v.length];
                            for (int i = 0; i < v.length; i++) {
                                v[i] = (float) e.getValue().get(i).asDouble();
                                w[(i + 1) % v.length] = v[i]; // 차원이 한 칸 밀린 벡터(틀린 구현을 흉내)
                            }
                            good.add(v);
                            bad.add(w);
                        });
        // 빈 문자열처럼 벡터가 0 에 가까운 항목은 비교에서 제외한다(코사인이 정의되지 않는다).
        List<String> usable = new ArrayList<>();
        List<float[]> goodUsable = new ArrayList<>();
        List<float[]> badUsable = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            double norm = 0;
            for (float x : good.get(i)) {
                norm += x * x;
            }
            if (norm > 0.5) {
                usable.add(texts.get(i));
                goodUsable.add(good.get(i));
                badUsable.add(bad.get(i));
            }
        }
        assertThat(usable).hasSizeGreaterThanOrEqualTo(3);

        try (OnnxEmbeddingAdapter adapter =
                new OnnxEmbeddingAdapter(
                        "tiny",
                        TINY.resolve("model.onnx"),
                        TINY.resolve("tokenizer.json"),
                        "",
                        128,
                        1)) {
            Summary ok =
                    compare(
                            "tiny",
                            "python onnxruntime",
                            usable,
                            goodUsable.toArray(new float[0][]),
                            adapter);
            assertThat(ok.min()).isGreaterThan(0.9999);
            assertThat(ok.verdict()).startsWith("일치");

            Summary broken =
                    compare("tiny", "일부러 틀린 값", usable, badUsable.toArray(new float[0][]), adapter);
            assertThat(broken.min()).isLessThan(0.99);
            assertThat(broken.verdict()).startsWith("불일치");
            assertThat(format(broken)).contains("불일치").contains("<details>");
        }
    }

    @Test
    @DisplayName("실제 모델: Java 어댑터의 임베딩이 sentence-transformers 정답 벡터와 같은지 확인한다")
    void realModelMatchesReference() throws Exception {
        String root = System.getenv("CHATBOT_EVAL_MODELS_DIR");
        assumeTrue(root != null && !root.isBlank(), "CHATBOT_EVAL_MODELS_DIR 가 없어 건너뜁니다");

        List<Path> dirs;
        try (Stream<Path> stream = Files.list(Path.of(root))) {
            dirs =
                    stream.filter(Files::isDirectory)
                            .filter(d -> Files.exists(d.resolve("golden.json")))
                            .filter(d -> Files.exists(d.resolve("model.onnx")))
                            .sorted()
                            .toList();
        }
        assumeTrue(!dirs.isEmpty(), "golden.json 이 있는 모델 디렉터리가 없어 건너뜁니다");

        double minCosine =
                Double.parseDouble(
                        System.getenv().getOrDefault("CHATBOT_GOLDEN_MIN_COSINE", "0.99"));
        boolean enforce =
                Boolean.parseBoolean(System.getenv().getOrDefault("CHATBOT_EVAL_ENFORCE", "true"));

        StringBuilder report = new StringBuilder("## 구현 정합성: Java 임베딩 vs 정답 벡터\n\n");
        List<String> failures = new ArrayList<>();
        for (Path dir : dirs) {
            JsonNode golden = MAPPER.readTree(Files.readString(dir.resolve("golden.json")));
            List<String> texts = new ArrayList<>();
            golden.get("texts").forEach(t -> texts.add(t.asText()));
            float[][] expected = vectors(golden.get("vectors"));
            String id = dir.getFileName().toString();
            try (OnnxEmbeddingAdapter adapter =
                    new OnnxEmbeddingAdapter(
                            id,
                            dir.resolve("model.onnx"),
                            dir.resolve("tokenizer.json"),
                            golden.path("prefix").asText(""),
                            golden.path("maxTokens").asInt(128),
                            2)) {
                Summary summary =
                        compare(id, golden.path("reference").asText("?"), texts, expected, adapter);
                report.append(format(summary));
                if (summary.min() < minCosine) {
                    failures.add(
                            String.format(
                                    Locale.ROOT,
                                    "%s: 최소 코사인 %.6f < 기준 %.3f",
                                    id,
                                    summary.min(),
                                    minCosine));
                }
            }
        }

        Path out = Path.of("build/chatbot-eval/golden.md");
        Files.createDirectories(out.getParent());
        Files.writeString(out, report.toString(), StandardCharsets.UTF_8);
        System.out.println(report);

        if (enforce) {
            assertThat(failures).as("정답 벡터와 불일치").isEmpty();
        }
    }
}
