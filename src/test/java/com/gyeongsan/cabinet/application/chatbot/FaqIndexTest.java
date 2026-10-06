package com.gyeongsan.cabinet.application.chatbot;

import static com.gyeongsan.cabinet.application.chatbot.ChatbotTestSupport.faq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gyeongsan.cabinet.domain.chatbot.model.Faq;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FaqIndexTest {

    private final Faq a = faq(1, "대여", "A 답변", "대표 질문 A", "다른 표현 A");
    private final Faq b = faq(2, "반납", "B 답변", "대표 질문 B");

    private FaqIndex index() {
        return new FaqIndex(
                List.of(
                        new FaqIndex.Entry(1, "대표 질문 A", new float[] {1f, 0f}),
                        new FaqIndex.Entry(1, "다른 표현 A", new float[] {0.6f, 0.8f}),
                        new FaqIndex.Entry(2, "대표 질문 B", new float[] {0f, 1f})),
                Map.of(1L, a, 2L, b));
    }

    @Test
    @DisplayName("점수 높은 순으로 돌려주고, 같은 FAQ 의 여러 표현 중에는 가장 비슷한 것 하나만 쓴다")
    void bestPerFaqSortedDescending() {
        // 질문 벡터 (0.6, 0.8): 다른 표현 A 와 정확히 같고(1.0), B 와는 0.8, 대표 질문 A 와는 0.6
        List<FaqIndex.Hit> hits = index().search(new float[] {0.6f, 0.8f}, 5);

        assertThat(hits).hasSize(2);
        assertThat(hits.get(0).faq()).isEqualTo(a);
        assertThat(hits.get(0).matchedQuestion()).isEqualTo("다른 표현 A");
        assertThat(hits.get(0).score()).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-6));
        assertThat(hits.get(1).faq()).isEqualTo(b);
        assertThat(hits.get(1).score()).isCloseTo(0.8, org.assertj.core.data.Offset.offset(1e-6));
    }

    @Test
    @DisplayName("limit 만큼만 돌려준다")
    void limit() {
        assertThat(index().search(new float[] {1f, 0f}, 1)).hasSize(1);
    }

    @Test
    @DisplayName("빈 색인은 빈 결과다")
    void empty() {
        assertThat(FaqIndex.EMPTY.search(new float[] {1f, 0f}, 3)).isEmpty();
        assertThat(FaqIndex.EMPTY.size()).isZero();
    }

    @Test
    @DisplayName("벡터 차원이 다르면 조용히 틀린 값을 내지 않고 예외를 던진다 (모델이 바뀐 경우)")
    void dimensionMismatch() {
        assertThatThrownBy(() -> index().search(new float[] {1f, 0f, 0f}, 3))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("차원");
    }
}
