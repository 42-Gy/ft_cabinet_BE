package com.gyeongsan.cabinet.application.chatbot;

import static com.gyeongsan.cabinet.application.chatbot.ChatbotTestSupport.faq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gyeongsan.cabinet.application.chatbot.ChatbotTestSupport.CountingMetrics;
import com.gyeongsan.cabinet.application.chatbot.ChatbotTestSupport.FakeEmbedding;
import com.gyeongsan.cabinet.application.chatbot.ChatbotTestSupport.FakeFaqRepository;
import com.gyeongsan.cabinet.domain.chatbot.model.ChatbotAnswer;
import com.gyeongsan.cabinet.domain.chatbot.model.ChatbotAnswer.Result;
import com.gyeongsan.cabinet.domain.chatbot.model.Faq;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatbotServiceTest {

    private FakeFaqRepository repository;
    private FakeEmbedding embedding;
    private CountingMetrics metrics;
    private FaqIndexManager manager;
    private ChatbotService service;

    private static final ChatbotSettings SETTINGS =
            new ChatbotSettings(0.80, 0.60, 3, 50, 2, "못 찾았어요");

    @BeforeEach
    void setUp() {
        repository = new FakeFaqRepository();
        embedding = new FakeEmbedding();
        metrics = new CountingMetrics();
        manager = new FaqIndexManager(repository, embedding);
        service = new ChatbotService(manager, embedding, metrics, SETTINGS);

        // FAQ 1(대여)은 x 축, FAQ 2(반납)는 y 축, FAQ 3(연장)은 z 축 방향.
        repository.save(
                new Faq(null, null, "대여", "대여 답변", true, List.of("대여 방법", "대여 어떻게"), null, null));
        repository.save(new Faq(null, null, "반납", "반납 답변", true, List.of("반납 방법"), null, null));
        repository.save(new Faq(null, null, "연장", "연장 답변", true, List.of("연장 방법"), null, null));
        embedding
                .on("대여 방법", 1, 0, 0)
                .on("대여 어떻게", 0.9, 0.1, 0)
                .on("반납 방법", 0, 1, 0)
                .on("연장 방법", 0, 0, 1);
    }

    private void ready() {
        manager.rebuild();
    }

    @Test
    @DisplayName("유사도가 임계값 이상이면 그 FAQ 의 답변을 그대로 돌려준다")
    void matched() {
        ready();
        embedding.on("사물함 빌리려면?", 0.95, 0.31, 0); // 대여 방법과 약 0.95

        ChatbotAnswer answer = service.ask("사물함 빌리려면?");

        assertThat(answer.result()).isEqualTo(Result.MATCHED);
        assertThat(answer.faq().answer()).isEqualTo("대여 답변");
        assertThat(answer.score()).isGreaterThanOrEqualTo(0.80);
        assertThat(metrics.count(Result.MATCHED)).isEqualTo(1);
    }

    @Test
    @DisplayName("임계값에는 못 미치지만 비슷한 구간이면 후보 질문들을 돌려준다 (답변은 주지 않는다)")
    void suggested() {
        ready();
        // 대여 방향과 약 0.70, 반납 방향과 약 0.70 -> 후보 2개
        embedding.on("애매한 질문", 0.7, 0.7, 0.1);

        ChatbotAnswer answer = service.ask("애매한 질문");

        assertThat(answer.result()).isEqualTo(Result.SUGGESTED);
        assertThat(answer.faq()).isNull();
        assertThat(answer.suggestions()).hasSize(2);
        assertThat(answer.suggestions().get(0).question()).isEqualTo("대여 방법");
        assertThat(metrics.count(Result.SUGGESTED)).isEqualTo(1);
    }

    @Test
    @DisplayName("너무 다르면 못 찾은 것으로 처리한다")
    void unmatched() {
        ready();

        ChatbotAnswer answer = service.ask("오늘 학식 뭐 나와요");

        assertThat(answer.result()).isEqualTo(Result.UNMATCHED);
        assertThat(answer.faq()).isNull();
        assertThat(answer.suggestions()).isEmpty();
        assertThat(metrics.count(Result.UNMATCHED)).isEqualTo(1);
    }

    @Test
    @DisplayName("임계값 경계: 정확히 임계값이면 찾은 것이고, 그보다 조금 낮으면 후보다")
    void thresholdBoundary() {
        ready();
        embedding.on("경계 질문", 0.8, 0, 0.6); // 대여 방법과 정확히 0.8 (다른 표현과는 약 0.795)

        assertThat(service.ask("경계 질문").result()).isEqualTo(Result.MATCHED);

        embedding.on("경계 아래", 0.79, 0, 0.6131); // 대여 방법과 0.79, 연장과 0.61
        assertThat(service.ask("경계 아래").result()).isEqualTo(Result.SUGGESTED);
    }

    @Test
    @DisplayName("빈 질문, 공백뿐인 질문, 너무 긴 질문은 400 이다")
    void invalidQuestions() {
        ready();

        for (String bad : new String[] {null, "", "   ", "가".repeat(51)}) {
            assertThatThrownBy(() -> service.ask(bad))
                    .isInstanceOfSatisfying(
                            ServiceException.class,
                            e ->
                                    assertThat(e.getErrorCode())
                                            .isEqualTo(ErrorCode.CHATBOT_INVALID_QUESTION));
        }
        assertThat(embedding.calls).doesNotContain("");
    }

    @Test
    @DisplayName("공백을 정리하고 NFC 로 통일해서 같은 질문으로 취급한다")
    void normalizesInput() {
        ready();
        embedding.on("대여 방법", 1, 0, 0);

        String decomposedWithSpaces =
                "  "
                        + java.text.Normalizer.normalize("대여   방법", java.text.Normalizer.Form.NFD)
                        + "  ";
        ChatbotAnswer answer = service.ask(decomposedWithSpaces);

        assertThat(answer.result()).isEqualTo(Result.MATCHED);
    }

    @Test
    @DisplayName("색인이 준비되기 전이면 503 이다")
    void notReady() {
        assertThatThrownBy(() -> service.ask("대여 방법"))
                .isInstanceOfSatisfying(
                        ServiceException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CHATBOT_NOT_READY));
        assertThat(metrics.counts).isEmpty();
    }

    @Test
    @DisplayName("질문을 임베딩하다 모델을 쓸 수 없게 되면 503 이다")
    void embeddingBecomesUnavailable() {
        ready();
        embedding.unavailable = true;

        assertThatThrownBy(() -> service.ask("대여 방법"))
                .isInstanceOfSatisfying(
                        ServiceException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CHATBOT_NOT_READY));
    }

    @Test
    @DisplayName("FAQ 가 하나도 없으면 못 찾은 것이다")
    void emptyIndex() {
        FakeFaqRepository empty = new FakeFaqRepository();
        FaqIndexManager emptyManager = new FaqIndexManager(empty, embedding);
        emptyManager.rebuild();

        ChatbotAnswer answer =
                new ChatbotService(emptyManager, embedding, metrics, SETTINGS).ask("아무 질문");

        assertThat(answer.result()).isEqualTo(Result.UNMATCHED);
    }

    @Test
    @DisplayName("동시에 임베딩을 계산하는 수를 제한한다: 모두 막혀 있으면 기다리다 503 으로 거절한다")
    void gateRejectsWhenSaturated() throws Exception {
        ready();
        CountDownLatch inside = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        FakeEmbedding blocking =
                new FakeEmbedding() {
                    @Override
                    public float[] embed(String text) {
                        if (text.startsWith("느린")) {
                            inside.countDown();
                            try {
                                release.await(10, TimeUnit.SECONDS);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                        return super.embed(text);
                    }
                };
        ChatbotService gated = new ChatbotService(manager, blocking, metrics, SETTINGS);

        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            Future<?> a = pool.submit(() -> gated.ask("느린 질문 1"));
            Future<?> b = pool.submit(() -> gated.ask("느린 질문 2"));
            assertThat(inside.await(5, TimeUnit.SECONDS)).isTrue();

            long started = System.nanoTime();
            assertThatThrownBy(() -> gated.ask("세 번째"))
                    .isInstanceOfSatisfying(
                            ServiceException.class,
                            e ->
                                    assertThat(e.getErrorCode())
                                            .isEqualTo(ErrorCode.CHATBOT_NOT_READY));
            long waitedMs = (System.nanoTime() - started) / 1_000_000;
            assertThat(waitedMs).isBetween(1_500L, 6_000L);

            release.countDown();
            a.get(5, TimeUnit.SECONDS);
            b.get(5, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("설정 검증: 임계값 순서나 범위가 잘못되면 거부한다")
    void settingsValidation() {
        for (double[] bad : new double[][] {{0.5, 0.6}, {1.2, 0.5}, {0.8, 0.0}, {0.8, -0.1}}) {
            assertThatThrownBy(() -> new ChatbotSettings(bad[0], bad[1], 3, 200, 2, "m"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> new ChatbotSettings(0.8, 0.6, 3, 200, 0, "m"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ChatbotSettings(0.8, 0.6, 3, 200, 2, " "))
                .isInstanceOf(IllegalArgumentException.class);
        // 같은 값은 허용(후보 구간 없음)
        assertThat(new ChatbotSettings(0.8, 0.8, 3, 200, 2, "m").suggestThreshold()).isEqualTo(0.8);
    }

    @SuppressWarnings("unused")
    private static Faq unused() {
        return faq(1, "x", "y", "z");
    }
}
