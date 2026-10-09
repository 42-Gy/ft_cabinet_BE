package com.gyeongsan.cabinet.adapter.out.embedding;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.gyeongsan.cabinet.domain.chatbot.model.EmbeddingUnavailableException;
import com.gyeongsan.cabinet.domain.chatbot.port.out.EmbeddingPort;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 서버 안에서 ONNX 모델로 문장 임베딩을 계산한다(외부 API 호출 없음, 질문이 서버 밖으로 나가지 않는다).
 *
 * <p>sentence-transformers 계열 모델의 ONNX 내보내기를 가정한다. 입력은 input_ids, attention_mask(,
 * token_type_ids)이고, 출력은 토큰별 은닉 상태 (평균 풀링한다)이거나 이미 풀링된 sentence_embedding 이다. 어느 쪽이든 마지막에 길이 1로
 * 정규화한다. 모델이 e5 계열이면 prefix 로 "query: "를 준다.
 */
public class OnnxEmbeddingAdapter implements EmbeddingPort, AutoCloseable {

    static final String INPUT_IDS = "input_ids";
    static final String ATTENTION_MASK = "attention_mask";
    static final String TOKEN_TYPE_IDS = "token_type_ids";
    static final String POOLED_OUTPUT = "sentence_embedding";

    private static final Set<String> SUPPORTED_INPUTS =
            Set.of(INPUT_IDS, ATTENTION_MASK, TOKEN_TYPE_IDS);

    private final String modelId;
    private final String prefix;
    private final OrtEnvironment environment;
    private final OrtSession session;
    private final HuggingFaceTokenizer tokenizer;
    private final Set<String> inputNames;
    private final Object tokenizerLock = new Object();

    public OnnxEmbeddingAdapter(
            String modelId,
            Path modelFile,
            Path tokenizerFile,
            String prefix,
            int maxTokens,
            int intraOpThreads) {
        this.modelId = modelId;
        this.prefix = prefix == null ? "" : prefix;
        try {
            if (!Files.isReadable(modelFile)) {
                throw new EmbeddingUnavailableException("모델 파일을 읽을 수 없습니다: " + modelFile);
            }
            if (!Files.isReadable(tokenizerFile)) {
                throw new EmbeddingUnavailableException("토크나이저 파일을 읽을 수 없습니다: " + tokenizerFile);
            }
            this.tokenizer =
                    HuggingFaceTokenizer.builder()
                            .optTokenizerPath(tokenizerFile)
                            .optAddSpecialTokens(true)
                            .optTruncation(true)
                            .optMaxLength(maxTokens)
                            .optPadding(false)
                            .build();

            this.environment = OrtEnvironment.getEnvironment("chatbot");
            try (OrtSession.SessionOptions options = new OrtSession.SessionOptions()) {
                options.setIntraOpNumThreads(intraOpThreads);
                options.setInterOpNumThreads(1);
                this.session = environment.createSession(modelFile.toString(), options);
            }
            this.inputNames = session.getInputNames();
            for (String name : inputNames) {
                if (!SUPPORTED_INPUTS.contains(name)) {
                    close();
                    throw new EmbeddingUnavailableException("지원하지 않는 모델 입력입니다: " + name);
                }
            }
        } catch (EmbeddingUnavailableException e) {
            throw e;
        } catch (Exception | LinkageError e) {
            throw new EmbeddingUnavailableException("임베딩 모델을 불러오지 못했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    public String modelId() {
        return modelId;
    }

    @Override
    public float[] embed(String text) {
        Encoding encoding;
        synchronized (tokenizerLock) {
            encoding = tokenizer.encode(prefix + text);
        }
        long[] ids = encoding.getIds();
        long[] mask = encoding.getAttentionMask();

        Map<String, OnnxTensor> inputs = new HashMap<>();
        try {
            inputs.put(INPUT_IDS, OnnxTensor.createTensor(environment, new long[][] {ids}));
            if (inputNames.contains(ATTENTION_MASK)) {
                inputs.put(
                        ATTENTION_MASK, OnnxTensor.createTensor(environment, new long[][] {mask}));
            }
            if (inputNames.contains(TOKEN_TYPE_IDS)) {
                // 한 문장만 넣으므로 모두 0 이다.
                inputs.put(
                        TOKEN_TYPE_IDS,
                        OnnxTensor.createTensor(environment, new long[][] {new long[ids.length]}));
            }
            inputs.keySet().retainAll(inputNames);

            try (OrtSession.Result result = session.run(inputs)) {
                return normalized(result, mask);
            }
        } catch (OrtException e) {
            throw new EmbeddingUnavailableException("임베딩 계산에 실패했습니다: " + e.getMessage(), e);
        } finally {
            inputs.values().forEach(OnnxTensor::close);
        }
    }

    private float[] normalized(OrtSession.Result result, long[] mask) throws OrtException {
        Optional<OnnxValue> pooled = result.get(POOLED_OUTPUT);
        OnnxValue value = pooled.orElseGet(() -> result.get(0));
        Object raw = value.getValue();

        float[] vector;
        if (raw instanceof float[][][] hidden) {
            vector = EmbeddingMath.meanPool(hidden[0], mask);
        } else if (raw instanceof float[][] twoDim) {
            vector = twoDim[0];
        } else {
            throw new EmbeddingUnavailableException(
                    "예상하지 못한 모델 출력 형태입니다: " + raw.getClass().getSimpleName());
        }
        return EmbeddingMath.normalize(vector);
    }

    @Override
    public void close() {
        try {
            if (session != null) {
                session.close();
            }
        } catch (OrtException ignored) {
            // 종료 중 오류는 무시한다.
        }
        if (tokenizer != null) {
            tokenizer.close();
        }
    }
}
