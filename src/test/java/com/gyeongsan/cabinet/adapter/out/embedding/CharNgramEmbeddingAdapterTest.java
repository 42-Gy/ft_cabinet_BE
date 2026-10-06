package com.gyeongsan.cabinet.adapter.out.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CharNgramEmbeddingAdapterTest {

    private final CharNgramEmbeddingAdapter adapter = new CharNgramEmbeddingAdapter();

    private double cosine(String a, String b) {
        float[] x = adapter.embed(a);
        float[] y = adapter.embed(b);
        double sum = 0;
        for (int i = 0; i < x.length; i++) {
            sum += (double) x[i] * y[i];
        }
        return sum;
    }

    @Test
    @DisplayName("벡터 길이는 1이고 같은 문장은 같다")
    void unitAndDeterministic() {
        float[] v = adapter.embed("사물함 대여 방법");
        double norm = 0;
        for (float x : v) {
            norm += (double) x * x;
        }

        assertThat(Math.sqrt(norm)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-5));
        assertThat(adapter.embed("사물함 대여 방법")).containsExactly(v);
    }

    @Test
    @DisplayName("글자가 많이 겹치면 더 비슷하고, 겹치지 않으면 덜 비슷하다 (의미는 모른다)")
    void overlapSimilarity() {
        double similar = cosine("사물함 대여 방법", "사물함 대여 어떻게 해요");
        double unrelated = cosine("사물함 대여 방법", "수박씨 강화 확률");

        assertThat(similar).isGreaterThan(unrelated);
        assertThat(cosine("사물함 대여", "사물함 대여"))
                .isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-5));
    }

    @Test
    @DisplayName("대소문자, 구두점, 자모 분리 입력은 같게 취급한다")
    void normalization() {
        assertThat(cosine("Cabinet Return!", "cabinet   return"))
                .isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-5));
        String nfd = java.text.Normalizer.normalize("대여 방법", java.text.Normalizer.Form.NFD);
        assertThat(cosine(nfd, "대여 방법")).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-5));
    }

    @Test
    @DisplayName("빈 문자열은 영벡터라 어떤 것과도 비슷하지 않다")
    void emptyText() {
        assertThat(cosine("", "사물함 대여")).isZero();
    }
}
