package com.gyeongsan.cabinet.application.chatbot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.gyeongsan.cabinet.application.chatbot.ChatbotEvalSupport.EvalItem;
import com.gyeongsan.cabinet.application.chatbot.ChatbotEvalSupport.OosProbe;
import com.gyeongsan.cabinet.application.chatbot.ChatbotEvalSupport.Probe;
import com.gyeongsan.cabinet.application.chatbot.ChatbotEvalSupport.Row;
import com.gyeongsan.cabinet.application.chatbot.ChatbotEvalSupport.RuleCell;
import com.gyeongsan.cabinet.application.chatbot.ChatbotEvalSupport.SeedFaq;
import com.gyeongsan.cabinet.application.chatbot.ChatbotEvalSupport.SetResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 평가 계산이 틀리면 모델 비교 자체가 틀리므로, 평가 도구의 계산을 작은 예로 검증한다. */
class ChatbotEvalSupportTest {

    private static Probe probe(
            String expected, List<String> alsoAccept, List<String> keys, List<Double> scores) {
        return new Probe(
                new EvalItem(expected, "q-" + expected, alsoAccept),
                keys,
                scores,
                keys.stream().map(k -> "변형-" + k).toList());
    }

    private static SetResult sample() {
        List<Probe> in =
                List.of(
                        // 1위 정답, 점수 높고 차이 큼
                        probe("a", null, List.of("a", "b", "c"), List.of(0.95, 0.80, 0.70)),
                        // 1위 오답, 정답은 2위 (같은 카테고리), 차이 작음
                        probe("a", null, List.of("b", "a", "c"), List.of(0.90, 0.89, 0.50)),
                        // 정답이 4위 → top-3 밖
                        probe(
                                "a",
                                null,
                                List.of("b", "c", "d", "a"),
                                List.of(0.85, 0.84, 0.83, 0.82)),
                        // alsoAccept 로 정답 인정
                        probe(
                                "a",
                                List.of("b"),
                                List.of("b", "c", "a"),
                                List.of(0.92, 0.60, 0.50)));
        List<OosProbe> out = List.of(new OosProbe("x", 0.88, 0.80), new OosProbe("y", 0.60, 0.50));
        return new SetResult(Map.of("a", "대여", "b", "대여", "c", "반납", "d", "반납"), in, out);
    }

    @Test
    @DisplayName("top-1/top-3/카테고리 적중률: 정답 인정 목록과 순위를 반영한다")
    void hitRates() {
        SetResult result = sample();
        assertThat(result.top1()).isEqualTo(0.5); // 1번, 4번
        assertThat(result.top3()).isEqualTo(0.75); // 1,2,4번 (3번은 4위)
        // 예측 카테고리가 정답 카테고리와 같은 것: 1(대여=대여), 2(b=대여), 3(b=대여), 4(b=대여) → 모두 같음
        assertThat(result.categoryTop1()).isEqualTo(1.0);
        assertThat(result.in().get(2).rank()).isEqualTo(4);
        assertThat(result.in().get(3).rank()).isEqualTo(1);
    }

    @Test
    @DisplayName("임계값 표: 커버리지, 1위 정답률, 후보 정답률, 범위 밖 수락을 센다")
    void thresholdRow() {
        Row row = ChatbotEvalSupport.row(sample(), 0.90);
        // 1위 점수 >= 0.90 인 질문: 0.95, 0.90, 0.92 → 3개
        assertThat(row.coverage()).isEqualTo(0.75);
        // 그 중 1위가 정답: 0.95(a), 0.92(b 인정) → 2/3
        assertThat(row.top1Precision()).isCloseTo(2.0 / 3, within(1e-9));
        // 후보(점수>=0.90 인 상위 3개)에 정답: 첫째 yes, 둘째 (b 0.90 만 후보, 정답 a 는 0.89라 제외) no, 넷째 yes → 2/3
        assertThat(row.top3Precision()).isCloseTo(2.0 / 3, within(1e-9));
        // 전체 중 후보에 정답이 있는 비율: 위 2개 + 셋째는 0.85 미만이라 후보 없음 → 2/4
        assertThat(row.recall()).isEqualTo(0.5);
        // 범위 밖: 0.88 < 0.90, 0.60 < 0.90 → 0
        assertThat(row.oosAccept()).isEqualTo(0.0);
        // 아무도 답하지 않으면 정밀도는 정의되지 않는다(NaN)
        assertThat(ChatbotEvalSupport.row(sample(), 0.99).top1Precision()).isNaN();
    }

    @Test
    @DisplayName("자동 답변 규칙: 점수와 margin 을 모두 만족한 것만 센다")
    void ruleCell() {
        // T=0.90, M=0.05: 0.95(margin .15) o, 0.90(margin .01) x, 0.92(margin .32) o → 커버리지 2/4, 둘
        // 다 정답
        RuleCell cell = ChatbotEvalSupport.rule(sample(), 0.90, 0.05);
        assertThat(cell.coverage()).isEqualTo(0.5);
        assertThat(cell.precision()).isEqualTo(1.0);
        assertThat(cell.oosAccept()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("AUROC 와 분위수")
    void signalMath() {
        assertThat(ChatbotEvalSupport.auroc(List.of(3.0, 4.0), List.of(1.0, 2.0))).isEqualTo(1.0);
        assertThat(ChatbotEvalSupport.auroc(List.of(1.0, 2.0), List.of(3.0, 4.0))).isEqualTo(0.0);
        assertThat(ChatbotEvalSupport.auroc(List.of(1.0), List.of(1.0))).isEqualTo(0.5);
        assertThat(ChatbotEvalSupport.auroc(List.of(), List.of(1.0))).isNaN();
        assertThat(ChatbotEvalSupport.quantile(new double[] {1, 2, 3, 4, 5}, 0.5)).isEqualTo(3.0);
        assertThat(ChatbotEvalSupport.quantile(new double[] {1, 2, 3, 4, 5}, 1.0)).isEqualTo(5.0);
    }

    @Test
    @DisplayName("학습곡선용 변형 선택: k 개만, offset 만큼 돌려서, 변형 수보다 많이 고르지 않는다")
    void variantSelection() {
        List<SeedFaq> faqs =
                List.of(
                        new SeedFaq("a", "c", "ans", true, List.of("a1", "a2", "a3", "a4")),
                        new SeedFaq("b", "c", "ans", true, List.of("b1", "b2")));
        assertThat(ChatbotEvalSupport.firstK(faqs, 2, 0))
                .containsExactly(List.of(0, 1), List.of(0, 1));
        assertThat(ChatbotEvalSupport.firstK(faqs, 2, 3))
                .containsExactly(List.of(3, 0), List.of(1, 0));
        assertThat(ChatbotEvalSupport.firstK(faqs, 4, 1))
                .containsExactly(List.of(1, 2, 3, 0), List.of(1, 0));
        assertThat(ChatbotEvalSupport.allVariants(faqs))
                .containsExactly(List.of(0, 1, 2, 3), List.of(0, 1));
    }

    @Test
    @DisplayName("혼동 쌍은 많이 나온 순으로 센다")
    void confusionPairs() {
        List<Probe> in =
                List.of(
                        probe("a", null, List.of("b", "a"), List.of(0.9, 0.8)),
                        probe("a", null, List.of("b", "a"), List.of(0.9, 0.8)),
                        probe("c", null, List.of("d", "c"), List.of(0.9, 0.8)),
                        probe("a", null, List.of("a", "b"), List.of(0.9, 0.8)));
        SetResult result =
                new SetResult(Map.of("a", "x", "b", "x", "c", "y", "d", "y"), in, List.of());
        assertThat(ChatbotEvalSupport.confusions(result))
                .extracting(Map.Entry::getKey, Map.Entry::getValue)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("a → b", 2),
                        org.assertj.core.groups.Tuple.tuple("c → d", 1));
    }
}
