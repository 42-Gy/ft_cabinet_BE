package com.gyeongsan.cabinet.adapter.out.embedding;

import com.gyeongsan.cabinet.domain.chatbot.port.out.EmbeddingPort;
import java.text.Normalizer;
import java.util.Locale;

/**
 * 글자 2~3-gram 을 해시로 모은 가벼운 벡터. **의미를 이해하지 못한다**(같은 글자가 겹쳐야 비슷하다고 본다). 모델 파일 없이 개발·테스트할 때와, 평가에서 실제
 * 모델이 "글자 겹침보다 얼마나 나은지" 비교하는 기준선으로만 쓴다. 운영 기본값은 ONNX 모델이다.
 */
public class CharNgramEmbeddingAdapter implements EmbeddingPort {

    static final int DIMENSION = 512;

    @Override
    public String modelId() {
        return "char-ngram-" + DIMENSION;
    }

    @Override
    public float[] embed(String text) {
        String cleaned =
                Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFC)
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^\\p{L}\\p{N}]+", " ")
                        .strip();
        float[] vector = new float[DIMENSION];
        for (String word : cleaned.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            String padded = "^" + word + "$";
            for (int n = 2; n <= 3; n++) {
                for (int i = 0; i + n <= padded.length(); i++) {
                    int hash = padded.substring(i, i + n).hashCode() * 0x9E3779B1;
                    int bucket = Math.floorMod(hash >>> 4, DIMENSION);
                    vector[bucket] += ((hash & 1) == 0) ? 1f : -1f;
                }
            }
        }
        return EmbeddingMath.normalize(vector);
    }
}
