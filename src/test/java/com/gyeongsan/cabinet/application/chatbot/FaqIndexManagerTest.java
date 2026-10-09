package com.gyeongsan.cabinet.application.chatbot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gyeongsan.cabinet.application.chatbot.ChatbotTestSupport.FakeEmbedding;
import com.gyeongsan.cabinet.application.chatbot.ChatbotTestSupport.FakeFaqRepository;
import com.gyeongsan.cabinet.domain.chatbot.model.EmbeddingUnavailableException;
import com.gyeongsan.cabinet.domain.chatbot.model.Faq;
import java.time.Duration;
import java.util.List;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FaqIndexManagerTest {

    private FakeFaqRepository repository;
    private FakeEmbedding embedding;
    private FaqIndexManager manager;

    @BeforeEach
    void setUp() {
        repository = new FakeFaqRepository();
        embedding = new FakeEmbedding();
        manager = new FaqIndexManager(repository, embedding);
        embedding.on("대여 방법", 1, 0, 0, 0).on("반납 방법", 0, 1, 0, 0);
    }

    private void save(String category, String answer, boolean enabled, String... questions) {
        repository.save(
                new Faq(null, null, category, answer, enabled, List.of(questions), null, null));
    }

    @Test
    @DisplayName("처음에는 준비되지 않은 상태이고, 만들면 사용 중인 FAQ 의 질문 표현이 모두 색인된다")
    void buildsIndexOfEnabledFaqs() {
        save("대여", "A", true, "대여 방법");
        save("반납", "B", true, "반납 방법");
        save("기타", "C", false, "숨긴 FAQ");
        assertThat(manager.isReady()).isFalse();

        manager.rebuild();

        assertThat(manager.isReady()).isTrue();
        assertThat(manager.current().size()).isEqualTo(2);
        assertThat(embedding.calls).doesNotContain("숨긴 FAQ");
    }

    @Test
    @DisplayName("만들기 전에 모델을 한 번 읽어 보아(워밍업) 모델 문제를 먼저 드러낸다")
    void warmsUpModelFirst() {
        save("대여", "A", true, "대여 방법");

        manager.rebuild();

        assertThat(embedding.calls.get(0)).isEqualTo("준비 확인");
    }

    @Test
    @DisplayName("모델을 쓸 수 없으면 준비되지 않은 채로 남는다 (예외는 호출한 쪽으로)")
    void modelUnavailable() {
        save("대여", "A", true, "대여 방법");
        embedding.unavailable = true;

        assertThatThrownBy(manager::rebuild).isInstanceOf(EmbeddingUnavailableException.class);

        assertThat(manager.isReady()).isFalse();
    }

    @Test
    @DisplayName("FAQ 가 바뀌면 refreshIfChanged 가 비동기로 색인을 다시 만든다")
    void refreshRebuildsOnlyWhenChanged() {
        save("대여", "A", true, "대여 방법");
        manager.rebuild();
        int callsAfterBuild = embedding.calls.size();

        manager.refreshIfChanged(); // 변화 없음
        pause();
        assertThat(embedding.calls).hasSize(callsAfterBuild);

        save("반납", "B", true, "반납 방법");
        manager.refreshIfChanged();

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(manager.current().size()).isEqualTo(2));
    }

    @Test
    @DisplayName("준비되지 않은 상태에서는 FAQ 가 그대로여도 refreshIfChanged 가 다시 시도한다")
    void retriesWhenNotReady() {
        save("대여", "A", true, "대여 방법");
        embedding.unavailable = true;
        manager.refreshIfChanged();
        pause();
        assertThat(manager.isReady()).isFalse();

        embedding.unavailable = false;
        manager.refreshIfChanged();

        Awaitility.await().atMost(Duration.ofSeconds(5)).until(manager::isReady);
    }

    @Test
    @DisplayName("재구축 요청이 몰려도 합쳐서 처리한다")
    void requestsAreCoalesced() {
        save("대여", "A", true, "대여 방법");

        for (int i = 0; i < 20; i++) {
            manager.requestRebuild();
        }

        Awaitility.await().atMost(Duration.ofSeconds(5)).until(manager::isReady);
        pause();
        long builds = embedding.calls.stream().filter("준비 확인"::equals).count();
        assertThat(builds).isBetween(1L, 3L);
    }

    @Test
    @DisplayName("FAQ 가 하나도 없어도 준비 완료(빈 색인)다")
    void emptyRepositoryIsReady() {
        manager.rebuild();

        assertThat(manager.isReady()).isTrue();
        assertThat(manager.current().size()).isZero();
    }

    @Test
    @DisplayName("NFC 로 정리한 뒤 임베딩한다 (자모가 분리된 입력도 같은 글자로)")
    void normalizesQuestionsBeforeEmbedding() {
        String decomposed = java.text.Normalizer.normalize("대여 방법", java.text.Normalizer.Form.NFD);
        save("대여", "A", true, decomposed);

        manager.rebuild();

        assertThat(embedding.calls).contains("대여 방법");
    }

    private static void pause() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
