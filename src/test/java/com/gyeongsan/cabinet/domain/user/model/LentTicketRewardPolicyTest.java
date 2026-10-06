package com.gyeongsan.cabinet.domain.user.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LentTicketRewardPolicyTest {

    private final LentTicketRewardPolicy policy = new LentTicketRewardPolicy(4800, 900);

    private static User user(String grade, boolean pisciner) {
        User user = User.of("test-user", "test@example.com", UserRole.USER, pisciner);
        user.updateFtGrade(grade);
        return user;
    }

    @Test
    @DisplayName("일반 사용자는 4800분(80시간) 이상일 때만 지급 대상이다 (경계값 4799 / 4800)")
    void defaultThreshold() {
        User cadet = user("Cadet", false);

        assertThat(policy.qualifies(cadet, 4799)).isFalse();
        assertThat(policy.qualifies(cadet, 4800)).isTrue();
        assertThat(policy.qualifies(cadet, 900)).isFalse();
    }

    @Test
    @DisplayName("트센은 900분(15시간) 이상이면 지급 대상이다 (경계값 899 / 900)")
    void transcenderThreshold() {
        User transcender = user("Transcender", false);

        assertThat(policy.qualifies(transcender, 899)).isFalse();
        assertThat(policy.qualifies(transcender, 900)).isTrue();
        assertThat(policy.qualifies(transcender, 4800)).isTrue();
    }

    @Test
    @DisplayName("grade 를 모르는 사용자(null, 미확인 값)는 일반 기준이다")
    void unknownGradeUsesDefault() {
        assertThat(policy.thresholdMinutesFor(user(null, false))).isEqualTo(4800);
        assertThat(policy.thresholdMinutesFor(user("Member", false))).isEqualTo(4800);
        assertThat(policy.thresholdMinutesFor(user("Transcender", false))).isEqualTo(900);
    }

    @Test
    @DisplayName("지급일 재조회 대상: 트센 기준 이상 일반 기준 미만의 비-트센, 피시너 제외")
    void gradeRefreshCandidates() {
        User cadet = user("Cadet", false);

        assertThat(policy.isGradeRefreshCandidate(cadet, 899)).isFalse();
        assertThat(policy.isGradeRefreshCandidate(cadet, 900)).isTrue();
        assertThat(policy.isGradeRefreshCandidate(cadet, 4799)).isTrue();
        assertThat(policy.isGradeRefreshCandidate(cadet, 4800)).isFalse();
        assertThat(policy.isGradeRefreshCandidate(user("Transcender", false), 1000)).isFalse();
        assertThat(policy.isGradeRefreshCandidate(user(null, true), 1000)).isFalse();
    }

    @Test
    @DisplayName("설정 실수는 생성 시점에 거부한다: 0 이하, 트센 기준이 더 큰 경우")
    void rejectsInvalidConfiguration() {
        assertThatThrownBy(() -> new LentTicketRewardPolicy(0, 900))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LentTicketRewardPolicy(4800, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LentTicketRewardPolicy(900, 4800))
                .isInstanceOf(IllegalArgumentException.class);
        // 같은 값은 허용(트센 혜택 없음)
        assertThat(new LentTicketRewardPolicy(900, 900).thresholdMinutesFor(user(null, false)))
                .isEqualTo(900);
    }
}
