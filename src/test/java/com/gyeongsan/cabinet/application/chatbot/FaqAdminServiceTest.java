package com.gyeongsan.cabinet.application.chatbot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gyeongsan.cabinet.application.chatbot.ChatbotTestSupport.FakeEmbedding;
import com.gyeongsan.cabinet.application.chatbot.ChatbotTestSupport.FakeFaqRepository;
import com.gyeongsan.cabinet.domain.chatbot.model.Faq;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import java.time.Duration;
import java.util.List;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FaqAdminServiceTest {

    private FakeFaqRepository repository;
    private FakeEmbedding embedding;
    private FaqIndexManager manager;
    private FaqAdminService service;

    @BeforeEach
    void setUp() {
        repository = new FakeFaqRepository();
        embedding = new FakeEmbedding();
        manager = new FaqIndexManager(repository, embedding);
        service = new FaqAdminService(repository, manager);
    }

    private static void assertCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        ServiceException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    @Test
    @DisplayName("추가하면 공백과 중복 질문이 정리되어 저장되고 색인 재구축이 요청된다")
    void createNormalizesAndRebuilds() {
        Faq created =
                service.create(
                        " 대여 ", "  답변 첫 줄\n둘째 줄  ", true, List.of("대여   방법", "대여 방법", " 빌리는 법 "));

        assertThat(created.id()).isNotNull();
        assertThat(created.category()).isEqualTo("대여");
        assertThat(created.answer()).isEqualTo("답변 첫 줄\n둘째 줄");
        assertThat(created.questions()).containsExactly("대여 방법", "빌리는 법");
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(manager::isReady);
    }

    @Test
    @DisplayName("잘못된 입력은 FAQ_INVALID(400) 로 거부한다")
    void validation() {
        List<String> ok = List.of("질문");
        assertCode(() -> service.create("", "답", true, ok), ErrorCode.FAQ_INVALID);
        assertCode(() -> service.create("분".repeat(31), "답", true, ok), ErrorCode.FAQ_INVALID);
        assertCode(() -> service.create("분류", " ", true, ok), ErrorCode.FAQ_INVALID);
        assertCode(() -> service.create("분류", "답".repeat(2001), true, ok), ErrorCode.FAQ_INVALID);
        assertCode(() -> service.create("분류", "답", true, List.of()), ErrorCode.FAQ_INVALID);
        assertCode(() -> service.create("분류", "답", true, null), ErrorCode.FAQ_INVALID);
        assertCode(() -> service.create("분류", "답", true, List.of(" ")), ErrorCode.FAQ_INVALID);
        assertCode(
                () -> service.create("분류", "답", true, List.of("질".repeat(201))),
                ErrorCode.FAQ_INVALID);
        List<String> eleven = new java.util.ArrayList<>();
        for (int i = 0; i < 11; i++) {
            eleven.add("질문 " + i);
        }
        assertCode(() -> service.create("분류", "답", true, eleven), ErrorCode.FAQ_INVALID);
        assertThat(repository.store).isEmpty();
    }

    @Test
    @DisplayName("수정은 id, 시드 키, 생성 시각을 유지하고 내용만 바꾼다")
    void updateKeepsIdentity() {
        Faq seeded =
                repository.save(
                        new Faq(null, "seed-1", "대여", "옛 답변", true, List.of("옛 질문"), null, null));

        Faq updated = service.update(seeded.id(), "반납", "새 답변", false, List.of("새 질문"));

        assertThat(updated.id()).isEqualTo(seeded.id());
        assertThat(updated.seedKey()).isEqualTo("seed-1");
        assertThat(updated.createdAt()).isEqualTo(seeded.createdAt());
        assertThat(updated.category()).isEqualTo("반납");
        assertThat(updated.enabled()).isFalse();
    }

    @Test
    @DisplayName("없는 FAQ 를 조회, 수정, 삭제하면 404 이다")
    void notFound() {
        assertCode(() -> service.get(99), ErrorCode.FAQ_NOT_FOUND);
        assertCode(
                () -> service.update(99, "분류", "답", true, List.of("질문")), ErrorCode.FAQ_NOT_FOUND);
        assertCode(() -> service.delete(99), ErrorCode.FAQ_NOT_FOUND);
    }

    @Test
    @DisplayName("삭제하면 사라진다")
    void delete() {
        Faq created = service.create("분류", "답", true, List.of("질문"));

        service.delete(created.id());

        assertThat(repository.store).isEmpty();
    }

    @Test
    @DisplayName("사용자 조회는 사용 중인 FAQ 만 보이고, 숨긴 FAQ 는 404 이다")
    void userViewHidesDisabled() {
        Faq visible = service.create("분류", "보이는 답", true, List.of("보이는 질문"));
        Faq hidden = service.create("분류", "숨긴 답", false, List.of("숨긴 질문"));

        assertThat(service.listEnabled()).extracting(Faq::id).containsExactly(visible.id());
        assertThat(service.getEnabled(visible.id()).answer()).isEqualTo("보이는 답");
        assertCode(() -> service.getEnabled(hidden.id()), ErrorCode.FAQ_NOT_FOUND);
        assertThat(service.listAll()).hasSize(2);
    }
}
