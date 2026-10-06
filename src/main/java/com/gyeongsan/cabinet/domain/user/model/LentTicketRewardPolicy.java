package com.gyeongsan.cabinet.domain.user.model;

/**
 * 월간 로그타임에 따른 대여권 지급 기준.
 *
 * <p>사용자마다 적용되는 기준은 하나다(트센이면 트센 기준, 아니면 일반 기준). 두 기준을 동시에 만족해도 평가가 한 번뿐이라 대여권은 많아야 1개다.
 *
 * @param defaultThresholdMinutes 일반 사용자 기준(분)
 * @param transcenderThresholdMinutes 트센 기준(분). 일반 기준보다 클 수 없다
 */
public record LentTicketRewardPolicy(int defaultThresholdMinutes, int transcenderThresholdMinutes) {

    public LentTicketRewardPolicy {
        // 설정 실수(0 이하)로 전원에게 지급되거나, 트센 기준이 더 높아지는 일을 부팅 시점에 막는다.
        if (defaultThresholdMinutes <= 0 || transcenderThresholdMinutes <= 0) {
            throw new IllegalArgumentException("대여권 지급 기준(분)은 1 이상이어야 합니다.");
        }
        if (transcenderThresholdMinutes > defaultThresholdMinutes) {
            throw new IllegalArgumentException("트센 지급 기준은 일반 기준보다 클 수 없습니다.");
        }
    }

    public int thresholdMinutesFor(User user) {
        return user.isTranscender() ? transcenderThresholdMinutes : defaultThresholdMinutes;
    }

    public boolean qualifies(User user, int logtimeMinutes) {
        return logtimeMinutes >= thresholdMinutesFor(user);
    }

    /**
     * 저장된 grade 가 오래되어 판정이 틀릴 수 있는 사용자인지. 새 기준의 영향을 받는 구간(트센 기준 이상, 일반 기준 미만)에 있는 비-트센 사용자만 지급일에
     * grade 를 다시 조회하면 된다. 피시너는 트센일 수 없으므로 제외한다.
     */
    public boolean isGradeRefreshCandidate(User user, int logtimeMinutes) {
        return !user.isPisciner()
                && !user.isTranscender()
                && logtimeMinutes >= transcenderThresholdMinutes
                && logtimeMinutes < defaultThresholdMinutes;
    }
}
