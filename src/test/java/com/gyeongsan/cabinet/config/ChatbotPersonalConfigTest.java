package com.gyeongsan.cabinet.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.gyeongsan.cabinet.domain.chatbot.model.PersonalIntent;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatbotPersonalConfigTest {

    @Test
    @DisplayName("개인화 질문 표현 목록은 모든 인텐트를 갖고 있고, 인텐트당 6개 이상이며 서로 겹치지 않는다")
    void catalogIsComplete() throws IOException {
        Map<PersonalIntent, List<String>> catalog = ChatbotPersonalConfig.loadCatalog();

        assertThat(catalog.keySet()).containsExactlyInAnyOrder(PersonalIntent.values());
        catalog.values().forEach(questions -> assertThat(questions).hasSizeGreaterThanOrEqualTo(6));
        List<String> all = catalog.values().stream().flatMap(List::stream).toList();
        assertThat(all).doesNotHaveDuplicates();
    }
}
