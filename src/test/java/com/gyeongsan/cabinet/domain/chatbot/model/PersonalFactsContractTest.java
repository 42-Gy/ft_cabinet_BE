package com.gyeongsan.cabinet.domain.chatbot.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 개인화 응답에 실릴 수 있는 필드 목록을 고정한다. 필드를 추가하려면 이 테스트를 고쳐야 하므로, 개인 정보가 응답에 새로 나가는 변경은 리뷰 없이 지나가지 못한다. */
class PersonalFactsContractTest {

    private static List<String> fields(Class<?> type) {
        return Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).toList();
    }

    @Test
    @DisplayName("인텐트별 응답 필드는 화이트리스트와 정확히 같다")
    void whitelist() {
        assertThat(fields(PersonalFacts.LentExpiry.class))
                .containsExactly(
                        "lentActive", "visibleNum", "expiredAt", "daysRemaining", "overdue");
        assertThat(fields(PersonalFacts.PenaltyStatus.class))
                .containsExactly("penaltyDays", "releaseDate");
        assertThat(fields(PersonalFacts.TicketCondition.class))
                .containsExactly(
                        "thresholdMinutes",
                        "monthlyLogtimeMinutes",
                        "meetsThreshold",
                        "hasUnusedTicket",
                        "nextPayDate");
        assertThat(fields(PersonalFacts.LentBlockers.class))
                .containsExactly(
                        "blockers",
                        "penaltyDays",
                        "penaltyReleaseDate",
                        "activeVisibleNum",
                        "allowedCabinets");
        assertThat(fields(PersonalAnswer.class)).containsExactly("intent", "message", "facts");
    }

    @Test
    @DisplayName("PersonalFacts 의 구현은 위 네 가지뿐이다")
    void sealedPermits() {
        assertThat(
                        Arrays.stream(PersonalFacts.class.getPermittedSubclasses())
                                .map(Class::getSimpleName)
                                .toList())
                .containsExactlyInAnyOrder(
                        "LentExpiry", "PenaltyStatus", "TicketCondition", "LentBlockers");
    }

    @Test
    @DisplayName("모든 인텐트에는 칩 문구가 있고 이름으로 찾을 수 있다")
    void intents() {
        Map<String, PersonalIntent> byName =
                Arrays.stream(PersonalIntent.values())
                        .collect(java.util.stream.Collectors.toMap(Enum::name, i -> i));
        assertThat(byName).hasSize(4);
        for (PersonalIntent intent : PersonalIntent.values()) {
            assertThat(intent.label()).isNotBlank();
            assertThat(PersonalIntent.fromName(intent.name())).contains(intent);
        }
        assertThat(PersonalIntent.fromName("lent_expiry")).isEmpty();
        assertThat(PersonalIntent.fromName("DROP TABLE")).isEmpty();
        assertThat(PersonalIntent.fromName(null)).isEmpty();
    }
}
