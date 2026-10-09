package com.gyeongsan.cabinet.application.chatbot;

import static org.assertj.core.api.Assertions.assertThat;

import com.gyeongsan.cabinet.adapter.in.web.chatbot.dto.ChatbotDtos.AskResponse;
import com.gyeongsan.cabinet.application.chatbot.ChatbotTestSupport.FakeEmbedding;
import com.gyeongsan.cabinet.application.chatbot.ChatbotTestSupport.FakeFaqRepository;
import com.gyeongsan.cabinet.domain.chatbot.model.ChatbotAnswer;
import com.gyeongsan.cabinet.domain.chatbot.model.ChatbotAnswer.Result;
import com.gyeongsan.cabinet.domain.chatbot.model.Faq;
import com.gyeongsan.cabinet.domain.chatbot.model.PersonalIntent;
import com.gyeongsan.cabinet.domain.chatbot.port.out.ChatbotMetricsPort;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 질문이 "내 정보" 질문처럼 보이면 칩만 제안하고(데이터 없음), 그렇지 않으면 기존 FAQ 동작이 그대로인지 본다. */
class ChatbotPersonalRoutingTest {

    private static final ChatbotSettings SETTINGS =
            new ChatbotSettings(0.80, 0.60, 3, 50, 2, "못 찾았어요");

    private final List<String> metricLog = new ArrayList<>();
    private final ChatbotMetricsPort metrics =
            new ChatbotMetricsPort() {
                @Override
                public void recordAsk(ChatbotAnswer.Result result) {}

                @Override
                public void recordPersonal(PersonalIntent intent, String outcome) {
                    metricLog.add(intent + ":" + outcome);
                }
            };

    private FakeEmbedding embedding;
    private FaqIndexManager manager;
    private ChatbotService withRouter;
    private ChatbotService withoutRouter;

    @BeforeEach
    void setUp() {
        FakeFaqRepository repository = new FakeFaqRepository();
        // 차원 6: 0~2 는 FAQ 축, 3~4 는 개인화 인텐트 축, 5 는 "아무와도 비슷하지 않음"
        embedding = new FakeEmbedding();
        embedding.dimension = 6;
        manager = new FaqIndexManager(repository, embedding);
        repository.save(new Faq(null, null, "대여", "대여 답변", true, List.of("대여 방법"), null, null));
        repository.save(new Faq(null, null, "반납", "반납 답변", true, List.of("반납 방법"), null, null));
        embedding.on("대여 방법", 1, 0, 0, 0, 0).on("반납 방법", 0, 1, 0, 0, 0);

        Map<PersonalIntent, List<String>> catalog = new EnumMap<>(PersonalIntent.class);
        catalog.put(PersonalIntent.LENT_EXPIRY, List.of("내 만료일"));
        catalog.put(PersonalIntent.PENALTY_STATUS, List.of("내 패널티"));
        embedding.on("내 만료일", 0, 0, 0, 1, 0).on("내 패널티", 0, 0, 0, 0, 1);
        PersonalIntentRouter router = new PersonalIntentRouter(catalog, embedding);

        withRouter = new ChatbotService(manager, embedding, metrics, SETTINGS, router);
        withoutRouter = new ChatbotService(manager, embedding, metrics, SETTINGS);
        manager.rebuild();
    }

    @Test
    @DisplayName("내 정보 질문이면 FAQ 결과와 함께 칩을 제안한다(데이터는 없다)")
    void chipShown() {
        embedding.on("내 사물함 언제까지야", 0, 0, 0, 0.97, 0.1);

        ChatbotAnswer answer = withRouter.ask("내 사물함 언제까지야");

        assertThat(answer.result()).isEqualTo(Result.UNMATCHED);
        assertThat(answer.personalAction()).isEqualTo(PersonalIntent.LENT_EXPIRY);
        assertThat(metricLog).containsExactly("LENT_EXPIRY:chip_shown");

        AskResponse response = AskResponse.from(answer, "못 찾았어요");
        assertThat(response.personalAction().intent()).isEqualTo("LENT_EXPIRY");
        assertThat(response.personalAction().label()).isEqualTo("내 사물함 만료일 확인");
    }

    @Test
    @DisplayName("일반 FAQ 질문에는 칩을 붙이지 않고 FAQ 답변은 그대로다")
    void faqQuestionHasNoChip() {
        embedding.on("사물함 빌리는 법", 0.97, 0, 0, 0, 0.05);

        ChatbotAnswer answer = withRouter.ask("사물함 빌리는 법");

        assertThat(answer.result()).isEqualTo(Result.MATCHED);
        assertThat(answer.personalAction()).isNull();
        assertThat(metricLog).isEmpty();
        assertThat(AskResponse.from(answer, "못 찾았어요").personalAction()).isNull();
    }

    @Test
    @DisplayName("FAQ 가 더 가까우면 인텐트가 기준을 넘어도 칩을 붙이지 않는다")
    void faqCloserThanIntent() {
        // 인텐트와 0.8 쯤 비슷하지만 FAQ(대여 방법)와는 거의 같다
        embedding.on("대여 만료일 관련", 0.9, 0, 0, 0.44, 0);

        ChatbotAnswer answer = withRouter.ask("대여 만료일 관련");

        assertThat(answer.result()).isEqualTo(Result.MATCHED);
        assertThat(answer.personalAction()).isNull();
    }

    @Test
    @DisplayName("인텐트와의 유사도가 후보 하한(suggest-threshold) 미만이면 칩을 붙이지 않는다")
    void belowThreshold() {
        // 어떤 인텐트와도 약 0.3(후보 하한 0.60 미만)
        embedding.on("애매한 말", 0, 0, 0, 0.3, 0.3, 0.9);

        ChatbotAnswer answer = withRouter.ask("애매한 말");

        assertThat(answer.personalAction()).isNull();
    }

    @Test
    @DisplayName("개인화가 꺼져 있으면(라우터 없음) 어떤 질문에도 칩이 없고 지표도 남기지 않는다")
    void disabled() {
        embedding.on("내 사물함 언제까지야", 0, 0, 0, 0.97, 0.1);

        ChatbotAnswer answer = withoutRouter.ask("내 사물함 언제까지야");

        assertThat(answer.personalAction()).isNull();
        assertThat(metricLog).isEmpty();
    }

    @Test
    @DisplayName("라우터는 모델을 쓸 수 없으면 비어 있고, 다시 쓸 수 있게 되면 그때 색인을 만든다")
    void routerRecovers() {
        Map<PersonalIntent, List<String>> catalog = new EnumMap<>(PersonalIntent.class);
        catalog.put(PersonalIntent.PENALTY_STATUS, List.of("내 패널티"));
        PersonalIntentRouter router = new PersonalIntentRouter(catalog, embedding);
        float[] query = embedding.embed("내 패널티");

        embedding.unavailable = true;
        assertThat(router.route(query)).isEmpty();

        embedding.unavailable = false;
        Optional<PersonalIntentRouter.Hit> hit = router.route(query);
        assertThat(hit).isPresent();
        assertThat(hit.get().intent()).isEqualTo(PersonalIntent.PENALTY_STATUS);
        assertThat(hit.get().score()).isGreaterThan(0.99);
    }
}
