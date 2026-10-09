package com.gyeongsan.cabinet.config;

import com.gyeongsan.cabinet.adapter.out.external.gemini.GeminiNoticeSummarizerAdapter;
import com.gyeongsan.cabinet.application.kakaonotify.SlackNoticeForwardService;
import com.gyeongsan.cabinet.application.kakaonotify.SlackNoticeSettings;
import com.gyeongsan.cabinet.domain.alarm.port.out.SlackChannelPort;
import com.gyeongsan.cabinet.domain.kakaonotify.port.in.ForwardSlackNoticesUseCase;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.KakaoConsentRepositoryPort;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.KakaoNotificationPort;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.NoticeCursorPort;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.NoticeSummarizerPort;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.TokenCipherPort;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.time.Clock;
import java.time.Duration;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 슬랙 공지 → 카카오톡 전달. SLACK_NOTICE_FORWARD_ENABLED=true 일 때만 켜지고(기본 꺼짐) 채널 ID 가 없으면 부팅이 실패한다. 카카오
 * 알림(KAKAO_NOTIFY_ENABLED)이 꺼져 있는데 켜면 "조용히 아무것도 안 보내는" 상태가 되지 않도록 부팅을 실패시킨다.
 */
@Log4j2
@Configuration
@ConditionalOnProperty(name = "app.slack-notice.enabled", havingValue = "true")
public class SlackNoticeConfig {

    @Bean
    public SlackNoticeSettings slackNoticeSettings(
            @Value("${app.slack-notice.channel-id:}") String channelId,
            @Value("${app.slack-notice.max-per-poll:5}") int maxPerPoll,
            @Value("${app.slack-notice.max-hold-hours:24}") int maxHoldHours,
            @Value("${app.slack-notice.link-url:}") String linkUrl,
            @Value("${app.slack-notice.permalink-enabled:false}") boolean permalinkEnabled) {
        return new SlackNoticeSettings(
                channelId.trim(), maxPerPoll, maxHoldHours, linkUrl.trim(), permalinkEnabled);
    }

    /**
     * 긴 공지 요약기. 꺼져 있거나(기본) API 키가 없으면 요약을 쓰지 않는 구현을 쓴다 — 키가 없어도 부팅은 막지 않고 경고만 남긴다(공지 전달 자체가 우선). 키는
     * 로그·예외에 남기지 않는다.
     */
    @Bean
    public NoticeSummarizerPort noticeSummarizerPort(
            @Value("${app.slack-notice.summary.enabled:false}") boolean enabled,
            @Value("${app.slack-notice.summary.api-key:}") String apiKey,
            @Value("${app.slack-notice.summary.model:gemini-3.8-flash}") String model,
            @Value("${app.slack-notice.summary.thinking-level:low}") String thinkingLevel,
            @Value("${app.slack-notice.summary.base-url:https://generativelanguage.googleapis.com}")
                    String baseUrl,
            @Value("${app.slack-notice.summary.timeout:8s}") String timeoutText,
            @Value("${app.slack-notice.summary.circuit-breaker.sliding-window-size:5}")
                    int slidingWindowSize,
            @Value("${app.slack-notice.summary.circuit-breaker.minimum-calls:3}") int minimumCalls,
            @Value("${app.slack-notice.summary.circuit-breaker.failure-rate-threshold:50}")
                    float failureRateThreshold,
            @Value("${app.slack-notice.summary.circuit-breaker.wait-in-open-state:10m}")
                    String waitInOpenStateText) {
        if (!enabled) {
            return NoticeSummarizerPort.disabled();
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn(
                    "[SlackNotice] SLACK_NOTICE_SUMMARY_ENABLED=true 이지만 GEMINI_API_KEY 가 없어 긴 공지 요약을 쓰지 않고 자르기로 대체합니다.");
            return NoticeSummarizerPort.disabled();
        }
        // "8s", "10m" 같은 표기를 읽는다(@Value 의 Duration 자동 변환은 Boot 의 변환 서비스가 있어야 해서 직접 파싱한다).
        Duration timeout = DurationStyle.detectAndParse(timeoutText.trim());
        Duration waitInOpenState = DurationStyle.detectAndParse(waitInOpenStateText.trim());
        CircuitBreaker circuitBreaker =
                CircuitBreaker.of(
                        "noticeSummary",
                        CircuitBreakerConfig.custom()
                                .slidingWindowSize(slidingWindowSize)
                                .minimumNumberOfCalls(minimumCalls)
                                .failureRateThreshold(failureRateThreshold)
                                .waitDurationInOpenState(waitInOpenState)
                                .build());
        log.info("[SlackNotice] 긴 공지 요약을 켭니다 - 모델: {}, 제한 시간: {}", model, timeout);
        return new GeminiNoticeSummarizerAdapter(
                GeminiNoticeSummarizerAdapter.createWebClient(baseUrl, timeout),
                model,
                apiKey,
                thinkingLevel,
                timeout,
                circuitBreaker);
    }

    @Bean
    public ForwardSlackNoticesUseCase forwardSlackNoticesUseCase(
            SlackChannelPort channelPort,
            NoticeCursorPort cursorPort,
            KakaoConsentRepositoryPort consentRepository,
            ObjectProvider<KakaoNotificationPort> kakao,
            ObjectProvider<TokenCipherPort> cipher,
            SlackNoticeSettings settings,
            NoticeSummarizerPort summarizer) {
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
                Clock.systemDefaultZone(),
                summarizer);
    }
}
