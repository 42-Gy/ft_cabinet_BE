package com.gyeongsan.cabinet.config;

import com.gyeongsan.cabinet.application.kakaonotify.SlackNoticeForwardService;
import com.gyeongsan.cabinet.application.kakaonotify.SlackNoticeSettings;
import com.gyeongsan.cabinet.domain.alarm.port.out.SlackChannelPort;
import com.gyeongsan.cabinet.domain.kakaonotify.port.in.ForwardSlackNoticesUseCase;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.KakaoConsentRepositoryPort;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.KakaoNotificationPort;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.NoticeCursorPort;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.TokenCipherPort;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 슬랙 공지 → 카카오톡 전달. SLACK_NOTICE_FORWARD_ENABLED=true 일 때만 켜지고(기본 꺼짐) 채널 ID 가 없으면 부팅이 실패한다. 카카오
 * 알림(KAKAO_NOTIFY_ENABLED)이 꺼져 있는데 켜면 "조용히 아무것도 안 보내는" 상태가 되지 않도록 부팅을 실패시킨다.
 */
@Configuration
@ConditionalOnProperty(name = "app.slack-notice.enabled", havingValue = "true")
public class SlackNoticeConfig {

    @Bean
    public SlackNoticeSettings slackNoticeSettings(
            @Value("${app.slack-notice.channel-id:}") String channelId,
            @Value("${app.slack-notice.max-per-poll:5}") int maxPerPoll,
            @Value("${app.slack-notice.max-hold-hours:24}") int maxHoldHours,
            @Value("${app.slack-notice.link-url:}") String linkUrl) {
        return new SlackNoticeSettings(channelId.trim(), maxPerPoll, maxHoldHours, linkUrl.trim());
    }

    @Bean
    public ForwardSlackNoticesUseCase forwardSlackNoticesUseCase(
            SlackChannelPort channelPort,
            NoticeCursorPort cursorPort,
            KakaoConsentRepositoryPort consentRepository,
            ObjectProvider<KakaoNotificationPort> kakao,
            ObjectProvider<TokenCipherPort> cipher,
            SlackNoticeSettings settings) {
        KakaoNotificationPort kakaoPort = kakao.getIfAvailable();
        TokenCipherPort cipherPort = cipher.getIfAvailable();
        if (kakaoPort == null || cipherPort == null) {
            throw new IllegalStateException(
                    "SLACK_NOTICE_FORWARD_ENABLED=true 이면 KAKAO_NOTIFY_ENABLED=true(와 KAKAO_TOKEN_ENC_KEY)도 필요합니다.");
        }
        // revoked_at 같은 LocalDateTime 기록이 동의 등록(consented_at)과 같은 시간대(서버 기본 Asia/Seoul)로 찍히도록
        // UTC 가 아니라 기본 시간대 시계를 쓴다.
        return new SlackNoticeForwardService(
                channelPort,
                cursorPort,
                consentRepository,
                kakaoPort,
                cipherPort,
                settings,
                Clock.systemDefaultZone());
    }
}
