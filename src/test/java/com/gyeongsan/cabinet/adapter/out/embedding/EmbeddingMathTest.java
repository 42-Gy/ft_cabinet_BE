package com.gyeongsan.cabinet.adapter.out.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmbeddingMathTest {

    @Test
    @DisplayName("평균 풀링은 attention_mask 가 1인 토큰만 평균낸다 (패딩 제외)")
    void meanPoolIgnoresPadding() {
        float[][] hidden = {{1f, 2f}, {3f, 4f}, {100f, 100f}};

        float[] pooled = EmbeddingMath.meanPool(hidden, new long[] {1, 1, 0});

        assertThat(pooled).containsExactly(2f, 3f);
    }

    @Test
    @DisplayName("마스크가 모두 0이거나 토큰이 없으면 예외다")
    void meanPoolRejectsEmpty() {
        assertThatThrownBy(() -> EmbeddingMath.meanPool(new float[][] {{1f}}, new long[] {0}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EmbeddingMath.meanPool(new float[0][], new long[0]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("정규화하면 길이가 1이 되고, 영벡터는 그대로 둔다")
    void normalize() {
        float[] unit = EmbeddingMath.normalize(new float[] {3f, 4f});
        assertThat(unit).containsExactly(0.6f, 0.8f);

        assertThat(EmbeddingMath.normalize(new float[] {0f, 0f})).containsExactly(0f, 0f);
    }

    @Test
    @DisplayName("정규화는 입력 배열을 바꾸지 않는다")
    void normalizeDoesNotMutate() {
        float[] input = {3f, 4f};

        EmbeddingMath.normalize(input);

        assertThat(input).containsExactly(3f, 4f);
    }
}
