package com.gyeongsan.cabinet.config;

import com.gyeongsan.cabinet.domain.user.model.LentTicketRewardPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RewardPolicyConfig {

    /** 잘못된 값(0 이하, 트센 기준 > 일반 기준)이면 정책 생성에서 예외가 나서 부팅이 실패한다. */
    @Bean
    public LentTicketRewardPolicy lentTicketRewardPolicy(
            @Value("${app.reward.lent-ticket.default-threshold-minutes:4800}")
                    int defaultThresholdMinutes,
            @Value("${app.reward.lent-ticket.transcender-threshold-minutes:900}")
                    int transcenderThresholdMinutes) {
        return new LentTicketRewardPolicy(defaultThresholdMinutes, transcenderThresholdMinutes);
    }
}
