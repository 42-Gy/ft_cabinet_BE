package com.gyeongsan.cabinet.adapter.out.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyeongsan.cabinet.domain.chatbot.model.EmbeddingUnavailableException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 실제 모델 대신 '장난감 ONNX 모델'(scripts/chatbot/make_tiny_model.py 로 만든 몇 KB 짜리)로 어댑터의 배선을 검증한다: 토크나이저 로딩,
 * 입력 이름 처리, 출력 형태, 평균 풀링, 정규화. 기대값은 같은 모델을 파이썬 onnxruntime 으로 계산한 값이다. 의미 검색 품질은 이 테스트가 아니라
 * 평가(ChatbotRetrievalEvaluationTest)가 본다.
 */
class OnnxEmbeddingAdapterTest {

    private static final Path DIR = Path.of("src/test/resources/fixtures/chatbot/tiny-model");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode expected() throws IOException {
        return MAPPER.readTree(Files.readString(DIR.resolve("expected.json")));
    }

    private static OnnxEmbeddingAdapter adapter(String modelName) {
        return new OnnxEmbeddingAdapter(
                modelName,
                DIR.resolve(modelName + ".onnx"),
                DIR.resolve("tokenizer.json"),
                "",
                128,
                1);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"model", "model-tti", "model-pooled"})
    @DisplayName("파이썬 onnxruntime 으로 계산한 기대 벡터와 같은 값을 돌려준다 (입력/출력 형태가 달라도)")
    void matchesPythonReference(String modelName) throws Exception {
        JsonNode vectors = expected().get("vectors").get(modelName);
        try (OnnxEmbeddingAdapter adapter = adapter(modelName)) {
            var fields = vectors.fields();
            int checked = 0;
            while (fields.hasNext()) {
                var entry = fields.next();
                float[] actual = adapter.embed(entry.getKey());
                JsonNode expectedVector = entry.getValue();

                assertThat(actual)
                        .as("%s / \"%s\"", modelName, entry.getKey())
                        .hasSize(expectedVector.size());
                for (int i = 0; i < actual.length; i++) {
                    assertThat((double) actual[i])
                            .as("%s / \"%s\" [%d]", modelName, entry.getKey(), i)
                            .isCloseTo(
                                    expectedVector.get(i).asDouble(),
                                    org.assertj.core.data.Offset.offset(1e-5));
                }
                checked++;
            }
            assertThat(checked).isGreaterThanOrEqualTo(4);
        }
    }

    @Test
    @DisplayName("벡터 길이는 항상 1이다 (내적이 곧 코사인 유사도)")
    void vectorsAreUnitLength() {
        try (OnnxEmbeddingAdapter adapter = adapter("model")) {
            for (String text : List.of("사물함 대여 어떻게 하나요", "how to return the cabinet", "모르는 단어들만")) {
                float[] v = adapter.embed(text);
                double norm = 0;
                for (float x : v) {
                    norm += (double) x * x;
                }
                assertThat(Math.sqrt(norm))
                        .as(text)
                        .isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-5));
            }
        }
    }

    @Test
    @DisplayName("같은 문장은 같은 벡터, 다른 문장은 다른 벡터다")
    void deterministicAndDistinct() {
        try (OnnxEmbeddingAdapter adapter = adapter("model")) {
            float[] a1 = adapter.embed("사물함 대여");
            float[] a2 = adapter.embed("사물함 대여");
            float[] b = adapter.embed("패널티 연체");

            assertThat(a1).containsExactly(a2);
            assertThat(a1).isNotEqualTo(b);
        }
    }

    @Test
    @DisplayName("접두어(e5 의 'query: ')는 입력 앞에 붙어 결과를 바꾼다")
    void prefixIsApplied() {
        Path model = DIR.resolve("model.onnx");
        Path tokenizer = DIR.resolve("tokenizer.json");
        try (OnnxEmbeddingAdapter plain =
                        new OnnxEmbeddingAdapter("m", model, tokenizer, "", 128, 1);
                OnnxEmbeddingAdapter prefixed =
                        new OnnxEmbeddingAdapter("m", model, tokenizer, "query: ", 128, 1)) {
            assertThat(prefixed.embed("사물함 대여")).isNotEqualTo(plain.embed("사물함 대여"));
        }
    }

    @Test
    @DisplayName("여러 스레드가 동시에 계산해도 결과가 같다 (스레드 안전)")
    void concurrentUse() throws Exception {
        try (OnnxEmbeddingAdapter adapter = adapter("model")) {
            float[] reference = adapter.embed("사물함 반납 연장 가능");
            ExecutorService pool = Executors.newFixedThreadPool(8);
            try {
                List<Future<float[]>> futures = new ArrayList<>();
                for (int i = 0; i < 64; i++) {
                    Callable<float[]> task = () -> adapter.embed("사물함 반납 연장 가능");
                    futures.add(pool.submit(task));
                }
                for (Future<float[]> f : futures) {
                    assertThat(f.get()).containsExactly(reference);
                }
            } finally {
                pool.shutdownNow();
            }
        }
    }

    @Test
    @DisplayName("모델이나 토크나이저 파일이 없으면 EmbeddingUnavailableException 이다")
    void missingFiles() {
        assertThatThrownBy(
                        () ->
                                new OnnxEmbeddingAdapter(
                                        "m",
                                        DIR.resolve("missing.onnx"),
                                        DIR.resolve("tokenizer.json"),
                                        "",
                                        128,
                                        1))
                .isInstanceOf(EmbeddingUnavailableException.class)
                .hasMessageContaining("모델 파일");
        assertThatThrownBy(
                        () ->
                                new OnnxEmbeddingAdapter(
                                        "m",
                                        DIR.resolve("model.onnx"),
                                        DIR.resolve("missing.json"),
                                        "",
                                        128,
                                        1))
                .isInstanceOf(EmbeddingUnavailableException.class)
                .hasMessageContaining("토크나이저");
    }

    @Test
    @DisplayName("ONNX 가 아닌 파일을 모델로 주면 EmbeddingUnavailableException 이다 (서버가 죽지 않는다)")
    void corruptModel() throws IOException {
        Path garbage = Files.createTempFile("not-a-model", ".onnx");
        try {
            Files.writeString(garbage, "this is not an onnx model");
            assertThatThrownBy(
                            () ->
                                    new OnnxEmbeddingAdapter(
                                            "m",
                                            garbage,
                                            DIR.resolve("tokenizer.json"),
                                            "",
                                            128,
                                            1))
                    .isInstanceOf(EmbeddingUnavailableException.class);
        } finally {
            Files.deleteIfExists(garbage);
        }
    }
}
