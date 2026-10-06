package com.gyeongsan.cabinet.application.chatbot;

import static org.assertj.core.api.Assertions.assertThat;

import com.gyeongsan.cabinet.application.chatbot.ChatbotTestSupport.FakeFaqRepository;
import com.gyeongsan.cabinet.application.chatbot.FaqSeeder.SeedItem;
import com.gyeongsan.cabinet.domain.chatbot.model.Faq;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FaqSeederTest {

    private final FakeFaqRepository repository = new FakeFaqRepository();
    private final FaqSeeder seeder = new FaqSeeder(repository);

    private static SeedItem item(String key, String q) {
        return new SeedItem(key, "분류", "답변 " + key, null, List.of(q));
    }

    @Test
    @DisplayName("테이블이 비어 있으면 넣고, 시드 키가 저장된다 (enabled 를 생략하면 사용 중)")
    void seedsWhenEmpty() {
        int inserted = seeder.seedIfEmpty(List.of(item("a", "질문 a"), item("b", "질문 b")));

        assertThat(inserted).isEqualTo(2);
        assertThat(repository.store.values()).extracting(Faq::seedKey).containsExactly("a", "b");
        assertThat(repository.store.values()).allMatch(Faq::enabled);
    }

    @Test
    @DisplayName("이미 데이터가 있으면(관리자가 고치거나 지운 뒤 포함) 아무것도 넣지 않는다")
    void doesNotSeedWhenNotEmpty() {
        seeder.seedIfEmpty(List.of(item("a", "질문 a")));
        repository.store.clear();
        repository.save(new Faq(null, null, "분류", "직접 추가", true, List.of("직접 질문"), null, null));

        assertThat(seeder.seedIfEmpty(List.of(item("a", "질문 a")))).isZero();
        assertThat(repository.store).hasSize(1);
    }

    @Test
    @DisplayName("잘못된 항목 하나가 있어도 나머지는 넣는다")
    void skipsInvalidItems() {
        int inserted =
                seeder.seedIfEmpty(
                        List.of(
                                item("a", "질문 a"),
                                new SeedItem("bad", "", "답", true, List.of("q")),
                                item("c", "질문 c")));

        assertThat(inserted).isEqualTo(2);
    }

    @Test
    @DisplayName("빈 목록이나 null 이면 아무것도 하지 않는다")
    void emptyInput() {
        assertThat(seeder.seedIfEmpty(List.of())).isZero();
        assertThat(seeder.seedIfEmpty(null)).isZero();
    }
}
