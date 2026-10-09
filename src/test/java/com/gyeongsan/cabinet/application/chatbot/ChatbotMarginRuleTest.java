package com.gyeongsan.cabinet.application.chatbot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gyeongsan.cabinet.adapter.in.web.chatbot.dto.ChatbotDtos.AskResponse;
import com.gyeongsan.cabinet.application.chatbot.ChatbotTestSupport.CountingMetrics;
import com.gyeongsan.cabinet.application.chatbot.ChatbotTestSupport.FakeEmbedding;
import com.gyeongsan.cabinet.application.chatbot.ChatbotTestSupport.FakeFaqRepository;
import com.gyeongsan.cabinet.domain.chatbot.model.ChatbotAnswer;
import com.gyeongsan.cabinet.domain.chatbot.model.ChatbotAnswer.Result;
import com.gyeongsan.cabinet.domain.chatbot.model.Faq;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 자동 답변 규칙(점수 ≥ T 그리고 1·2위 차이 ≥ M)과 대안 후보. e5 처럼 점수가 좁게 몰리는 모델에서는 점수가 높아도 2위와 거의 같은 경우가 많아서, 그럴 때
 * 답변을 바로 보여 주지 않고 후보로 돌리는지 확인한다. FAQ A(0도), B(20도), C(90도)를 평면 위의 방향으로 두고 질문을 각도로 놓는다.
 */
class ChatbotMarginRuleTest {

    private FakeEmbedding embedding;
    private FakeFaqRepository repository;
    private FaqIndexManager manager;

    private static final ChatbotSettings SETTINGS =
            new ChatbotSettings(0.90, 0.85, 3, 0.03, 2, 50, 2, "못 찾았어요");

    private static double[] at(double degrees) {
        double r = Math.toRadians(degrees);
        return new double[] {Math.cos(r), Math.sin(r)};
    }

    @BeforeEach
    void setUp() {
        repository = new FakeFaqRepository();
        embedding = new FakeEmbedding();
        manager = new FaqIndexManager(repository, embedding);
        repository.save(new Faq(null, null, "대여", "A 답변", true, List.of("A 질문"), null, null));
        repository.save(new Faq(null, null, "반납", "B 답변", true, List.of("B 질문"), null, null));
        repository.save(new Faq(null, null, "연장", "C 답변", true, List.of("C 질문"), null, null));
        embedding.on("A 질문", at(0)).on("B 질문", at(20)).on("C 질문", at(90));
        manager.rebuild();
    }

    private ChatbotService service(ChatbotSettings settings) {
        return new ChatbotService(manager, embedding, new CountingMetrics(), settings);
    }

    @Test
    @DisplayName("점수가 높고 2위와 차이도 크면 답변하고, 점수가 후보 하한 이상인 2위를 대안으로 함께 준다")
    void matchedWithAlternatives() {
        embedding.on("명확한 질문", at(-5)); // A 0.996, B 0.906(차이 0.09), C 약 -0.09

        ChatbotAnswer answer = service(SETTINGS).ask("명확한 질문");

        assertThat(answer.result()).isEqualTo(Result.MATCHED);
        assertThat(answer.faq().answer()).isEqualTo("A 답변");
        assertThat(answer.suggestions())
                .extracting(ChatbotAnswer.Suggestion::question)
                .containsExactly("B 질문"); // C 는 후보 하한(0.85) 미만이라 빠진다
    }

    @Test
    @DisplayName("점수가 임계값 이상이어도 1·2위 차이가 margin 보다 작으면 답변하지 않고 후보로 돌린다")
    void highScoreButTinyMarginBecomesSuggested() {
        embedding.on("헷갈리는 질문", at(8)); // A 0.990, B 0.978(차이 0.012)

        ChatbotAnswer answer = service(SETTINGS).ask("헷갈리는 질문");

        assertThat(answer.result()).isEqualTo(Result.SUGGESTED);
        assertThat(answer.faq()).isNull();
        assertThat(answer.suggestions())
                .extracting(ChatbotAnswer.Suggestion::question)
                .containsExactly("A 질문", "B 질문");
    }

    @Test
    @DisplayName("margin 경계: 차이가 margin 이상이면 답변, 조금 모자라면 후보")
    void marginBoundary() {
        // A, B 가 20도 벌어져 있으므로 질문 각도 t 에서 차이 = cos(t) - cos(20-t)
        // t=-1: 0.99985 - 0.93358 = 0.0663, 큰 쪽이 A
        embedding.on("차이 큼", at(-1));
        assertThat(service(SETTINGS).ask("차이 큼").result()).isEqualTo(Result.MATCHED);

        // t=5: 0.99619 - 0.96593 = 0.0303 (margin 0.03 이상 → 답변)
        embedding.on("차이 간신히", at(5));
        assertThat(service(SETTINGS).ask("차이 간신히").result()).isEqualTo(Result.MATCHED);

        // t=6: 0.99452 - 0.97030 = 0.0242 (margin 미만 → 후보)
        embedding.on("차이 모자람", at(6));
        assertThat(service(SETTINGS).ask("차이 모자람").result()).isEqualTo(Result.SUGGESTED);
    }

    @Test
    @DisplayName("1위 점수가 답변 임계값 미만이면 margin 과 상관없이 후보 하한만 본다")
    void belowMatchThreshold() {
        embedding.on("아래 질문", at(-30)); // A 0.866, B 0.643
        ChatbotAnswer suggested = service(SETTINGS).ask("아래 질문");
        assertThat(suggested.result()).isEqualTo(Result.SUGGESTED);
        assertThat(suggested.suggestions()).extracting(s -> s.question()).containsExactly("A 질문");

        embedding.on("멀리 있는 질문", at(-70)); // A 0.342
        assertThat(service(SETTINGS).ask("멀리 있는 질문").result()).isEqualTo(Result.UNMATCHED);
    }

    @Test
    @DisplayName("대안 개수 0 이면 답변에 대안을 붙이지 않는다")
    void noAlternativesWhenDisabled() {
        ChatbotSettings noAlternatives = new ChatbotSettings(0.90, 0.85, 3, 0.03, 0, 50, 2, "m");
        embedding.on("명확한 질문", at(-5));

        ChatbotAnswer answer = service(noAlternatives).ask("명확한 질문");

        assertThat(answer.result()).isEqualTo(Result.MATCHED);
        assertThat(answer.suggestions()).isEmpty();
    }

    @Test
    @DisplayName("대안은 matchAlternatives 개까지만 준다")
    void alternativesAreCapped() {
        // B(20도)와 가까운 FAQ 를 하나 더 둔다(10도): 질문이 -3도면 A, D, B 순으로 높다
        repository.save(new Faq(null, null, "이사", "D 답변", true, List.of("D 질문"), null, null));
        embedding.on("D 질문", at(10));
        manager.rebuild();
        ChatbotSettings oneAlternative = new ChatbotSettings(0.90, 0.85, 3, 0.03, 1, 50, 2, "m");
        embedding.on("질문", at(-3)); // A 0.9986, D 0.9744, B 0.9205 -> 차이 0.024 로 margin 미달
        embedding.on("질문2", at(-6)); // A 0.9945, D 0.9613, B 0.8988 -> 차이 0.033

        ChatbotAnswer answer = service(oneAlternative).ask("질문2");

        assertThat(answer.result()).isEqualTo(Result.MATCHED);
        assertThat(answer.suggestions()).hasSize(1);
        assertThat(answer.suggestions().get(0).question()).isEqualTo("D 질문");
        assertThat(service(oneAlternative).ask("질문").result()).isEqualTo(Result.SUGGESTED);
    }

    @Test
    @DisplayName("FAQ 가 하나뿐이면 2위가 없으므로 margin 은 점수 자체와 같게 본다")
    void singleFaqHasNoSecond() {
        FakeFaqRepository single = new FakeFaqRepository();
        single.save(new Faq(null, null, "대여", "A 답변", true, List.of("A 질문"), null, null));
        FaqIndexManager singleManager = new FaqIndexManager(single, embedding);
        singleManager.rebuild();
        embedding.on("정확한 질문", at(0));

        ChatbotAnswer answer =
                new ChatbotService(singleManager, embedding, new CountingMetrics(), SETTINGS)
                        .ask("정확한 질문");

        assertThat(answer.result()).isEqualTo(Result.MATCHED);
        assertThat(answer.suggestions()).isEmpty();
    }

    @Test
    @DisplayName("응답 DTO: 자동 답변에도 대안 후보가 담기고, 후보 제안에는 답변이 없다")
    void responseCarriesAlternatives() {
        embedding.on("명확한 질문", at(-5)).on("헷갈리는 질문", at(8));

        AskResponse matched = AskResponse.from(service(SETTINGS).ask("명확한 질문"), "m");
        assertThat(matched.result()).isEqualTo("MATCHED");
        assertThat(matched.answer().answer()).isEqualTo("A 답변");
        assertThat(matched.suggestions()).hasSize(1);

        AskResponse suggested = AskResponse.from(service(SETTINGS).ask("헷갈리는 질문"), "m");
        assertThat(suggested.result()).isEqualTo("SUGGESTED");
        assertThat(suggested.answer()).isNull();
        assertThat(suggested.suggestions()).hasSize(2);
    }

    @Test
    @DisplayName("설정 검증: margin 은 0 이상 1 미만, 대안 개수는 0 이상")
    void settingsValidation() {
        assertThatThrownBy(() -> new ChatbotSettings(0.9, 0.85, 3, -0.01, 2, 50, 2, "m"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ChatbotSettings(0.9, 0.85, 3, 1.0, 2, 50, 2, "m"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ChatbotSettings(0.9, 0.85, 3, 0.03, -1, 50, 2, "m"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new ChatbotSettings(0.9, 0.85, 3, 0, 0, 50, 2, "m").matchMargin()).isZero();
    }
}
