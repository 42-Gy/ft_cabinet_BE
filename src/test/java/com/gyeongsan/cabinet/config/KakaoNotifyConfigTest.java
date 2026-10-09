package com.gyeongsan.cabinet.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.gyeongsan.cabinet.adapter.in.scheduler.kakaonotify.SlackNoticeScheduler;
import com.gyeongsan.cabinet.adapter.in.web.kakaonotify.KakaoNotifyController;
import com.gyeongsan.cabinet.adapter.out.external.gemini.GeminiNoticeSummarizerAdapter;
import com.gyeongsan.cabinet.adapter.out.external.kakao.KakaoNotificationAdapter;
import com.gyeongsan.cabinet.application.kakaonotify.SlackNoticeSettings;
import com.gyeongsan.cabinet.domain.alarm.port.out.SlackChannelPort;
import com.gyeongsan.cabinet.domain.auth.port.out.LinkRedirectUriPort;
import com.gyeongsan.cabinet.domain.kakaonotify.port.in.ForwardSlackNoticesUseCase;
import com.gyeongsan.cabinet.domain.kakaonotify.port.in.GrantKakaoNotifyConsentUseCase;
import com.gyeongsan.cabinet.domain.kakaonotify.port.in.KakaoNotifySettingsUseCase;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.KakaoConsentRepositoryPort;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.KakaoLoginLinkPort;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.KakaoNotificationPort;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.NoticeCursorPort;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.NoticeSummarizerPort;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.TokenCipherPort;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import java.io.IOException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

/** 운영 application.yml 기본값 그대로(꺼짐), 켰을 때의 필수 설정 검증, 서로 의존하는 두 스위치의 조합을 확인한다. */
class KakaoNotifyConfigTest {

    private static final Path MAIN_YML = Path.of("src/main/resources/application.yml");

    private static String key() {
        byte[] k = new byte[32];
        new SecureRandom().nextBytes(k);
        return Base64.getEncoder().encodeToString(k);
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withUserConfiguration(KakaoNotifyConfig.class, SlackNoticeConfig.class)
                .withBean(KakaoLoginLinkPort.class, () -> mock(KakaoLoginLinkPort.class))
                .withBean(LinkRedirectUriPort.class, () -> mock(LinkRedirectUriPort.class))
                .withBean(KakaoNotificationPort.class, () -> mock(KakaoNotificationPort.class))
                .withBean(
                        KakaoConsentRepositoryPort.class,
                        () -> mock(KakaoConsentRepositoryPort.class))
                .withBean(UserRepositoryPort.class, () -> mock(UserRepositoryPort.class))
                .withBean(SlackChannelPort.class, () -> mock(SlackChannelPort.class))
                .withBean(NoticeCursorPort.class, () -> mock(NoticeCursorPort.class))
                .withInitializer(
                        context -> {
                            try {
                                for (PropertySource<?> source :
                                        new YamlPropertySourceLoader()
                                                .load(
                                                        "main-application-yml",
                                                        new FileSystemResource(MAIN_YML))) {
                                    context.getEnvironment().getPropertySources().addLast(source);
                                }
                            } catch (IOException e) {
                                throw new IllegalStateException(e);
                            }
                        });
    }

    @Test
    @DisplayName("운영 application.yml 기본값에서는 둘 다 꺼져 있어 아무 빈도 만들어지지 않는다")
    void disabledByDefault() {
        runner().run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context).doesNotHaveBean(TokenCipherPort.class);
                            assertThat(context)
                                    .doesNotHaveBean(GrantKakaoNotifyConsentUseCase.class);
                            assertThat(context).doesNotHaveBean(KakaoNotifySettingsUseCase.class);
                            assertThat(context).doesNotHaveBean(ForwardSlackNoticesUseCase.class);
                            assertThat(context).doesNotHaveBean(KakaoNotifyController.class);
                            assertThat(context).doesNotHaveBean(SlackNoticeScheduler.class);
                            assertThat(context).doesNotHaveBean(KakaoNotificationAdapter.class);
                        });
    }

    @Test
    @DisplayName("KAKAO_NOTIFY_ENABLED=true 이면 올바른 암호화 키로 동의/스위치 기능이 켜진다")
    void enabledWithKey() {
        runner().withPropertyValues("KAKAO_NOTIFY_ENABLED=true", "KAKAO_TOKEN_ENC_KEY=" + key())
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context).hasSingleBean(TokenCipherPort.class);
                            assertThat(context).hasSingleBean(GrantKakaoNotifyConsentUseCase.class);
                            assertThat(context).hasSingleBean(KakaoNotifySettingsUseCase.class);
                            // 공지 전달은 따로 켜야 한다.
                            assertThat(context).doesNotHaveBean(ForwardSlackNoticesUseCase.class);
                        });
    }

    @Test
    @DisplayName("켰는데 암호화 키가 없거나 형식이 틀리면 부팅이 실패하고, 실패 메시지에 키 값이 나오지 않는다")
    void enabledWithoutValidKeyFailsStartup() {
        runner().withPropertyValues("KAKAO_NOTIFY_ENABLED=true")
                .run(context -> assertThat(context).hasFailed());
        runner().withPropertyValues(
                        "KAKAO_NOTIFY_ENABLED=true", "KAKAO_TOKEN_ENC_KEY=too-short-secret")
                .run(
                        context -> {
                            assertThat(context).hasFailed();
                            assertThat(rootMessages(context.getStartupFailure()))
                                    .doesNotContain("too-short-secret");
                        });
    }

    private static String rootMessages(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable c = t; c != null; c = c.getCause()) {
            sb.append(c.getMessage()).append('\n');
        }
        return sb.toString();
    }

    @Test
    @DisplayName("두 스위치와 채널 ID 를 모두 주면 공지 전달이 켜지고, 링크 기본값은 FRONTEND_URL 이다")
    void noticeEnabled() {
        runner().withPropertyValues(
                        "KAKAO_NOTIFY_ENABLED=true",
                        "KAKAO_TOKEN_ENC_KEY=" + key(),
                        "SLACK_NOTICE_FORWARD_ENABLED=true",
                        "SLACK_NOTICE_CHANNEL_ID= C0NOTICE ",
                        "FRONTEND_URL=https://front.example")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context).hasSingleBean(ForwardSlackNoticesUseCase.class);
                            SlackNoticeSettings settings =
                                    context.getBean(SlackNoticeSettings.class);
                            assertThat(settings.channelId()).isEqualTo("C0NOTICE");
                            assertThat(settings.linkUrl()).isEqualTo("https://front.example");
                            assertThat(settings.maxPerPoll()).isEqualTo(5);
                            assertThat(settings.permalinkEnabled()).as("퍼머링크는 기본 꺼짐").isFalse();
                        });
    }

    @Test
    @DisplayName("SLACK_NOTICE_PERMALINK_ENABLED=true 로 퍼머링크를 켤 수 있다(기본은 꺼짐)")
    void permalinkCanBeSwitchedOn() {
        runner().withPropertyValues(
                        "KAKAO_NOTIFY_ENABLED=true",
                        "KAKAO_TOKEN_ENC_KEY=" + key(),
                        "SLACK_NOTICE_FORWARD_ENABLED=true",
                        "SLACK_NOTICE_CHANNEL_ID=C0NOTICE",
                        "SLACK_NOTICE_PERMALINK_ENABLED=true",
                        "FRONTEND_URL=https://front.example")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(
                                            context.getBean(SlackNoticeSettings.class)
                                                    .permalinkEnabled())
                                    .isTrue();
                        });
    }

    @Test
    @DisplayName("공지 전달만 켜고 카카오 알림을 안 켰으면 부팅이 실패한다(조용히 아무것도 안 보내는 상태 방지)")
    void noticeWithoutKakaoFails() {
        runner().withPropertyValues(
                        "SLACK_NOTICE_FORWARD_ENABLED=true",
                        "SLACK_NOTICE_CHANNEL_ID=C0NOTICE",
                        "FRONTEND_URL=https://front.example")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("공지 전달을 켰는데 채널 ID 나 링크 주소가 없으면 부팅이 실패한다")
    void noticeWithoutChannelOrLinkFails() {
        runner().withPropertyValues(
                        "KAKAO_NOTIFY_ENABLED=true",
                        "KAKAO_TOKEN_ENC_KEY=" + key(),
                        "SLACK_NOTICE_FORWARD_ENABLED=true",
                        "FRONTEND_URL=https://front.example")
                .run(context -> assertThat(context).hasFailed());
        runner().withPropertyValues(
                        "KAKAO_NOTIFY_ENABLED=true",
                        "KAKAO_TOKEN_ENC_KEY=" + key(),
                        "SLACK_NOTICE_FORWARD_ENABLED=true",
                        "SLACK_NOTICE_CHANNEL_ID=C0NOTICE",
                        "FRONTEND_URL=")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName(
            "공지 포워더의 시계는 서버 기본 시간대(Asia/Seoul)를 따른다 — UTC 면 revoked_at 이 consented_at 과 9시간 어긋난다")
    void noticeForwarderClockFollowsDefaultTimeZone() {
        java.util.TimeZone original = java.util.TimeZone.getDefault();
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Seoul"));
        try {
            runner().withPropertyValues(
                            "KAKAO_NOTIFY_ENABLED=true",
                            "KAKAO_TOKEN_ENC_KEY=" + key(),
                            "SLACK_NOTICE_FORWARD_ENABLED=true",
                            "SLACK_NOTICE_CHANNEL_ID=C0NOTICE",
                            "FRONTEND_URL=https://front.example")
                    .run(
                            context -> {
                                Object service = context.getBean(ForwardSlackNoticesUseCase.class);
                                java.time.Clock clock =
                                        (java.time.Clock)
                                                org.springframework.test.util.ReflectionTestUtils
                                                        .getField(service, "clock");
                                assertThat(clock.getZone())
                                        .isEqualTo(java.time.ZoneId.of("Asia/Seoul"));
                            });
        } finally {
            java.util.TimeZone.setDefault(original);
        }
    }

    private ApplicationContextRunner noticeRunner() {
        return runner().withPropertyValues(
                        "KAKAO_NOTIFY_ENABLED=true",
                        "KAKAO_TOKEN_ENC_KEY=" + key(),
                        "SLACK_NOTICE_FORWARD_ENABLED=true",
                        "SLACK_NOTICE_CHANNEL_ID=C0NOTICE",
                        "FRONTEND_URL=https://front.example");
    }

    @Test
    @DisplayName("긴 공지 요약은 기본 꺼짐이다 - 요약을 쓰지 않는 구현이 들어가 호출해도 항상 빈 값이다")
    void summaryDisabledByDefault() {
        noticeRunner()
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            NoticeSummarizerPort port = context.getBean(NoticeSummarizerPort.class);
                            assertThat(port).isNotInstanceOf(GeminiNoticeSummarizerAdapter.class);
                            assertThat(port.summarize("본문", 150)).isEmpty();
                        });
    }

    @Test
    @DisplayName("요약을 켰는데 GEMINI_API_KEY 가 없으면 부팅은 되고(공지 전달 우선) 요약만 꺼진 채 경고를 남긴다")
    void summaryEnabledWithoutKeyFallsBack() {
        org.slf4j.Logger slf4j = org.slf4j.LoggerFactory.getLogger(SlackNoticeConfig.class);
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) slf4j;
        ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender =
                new ch.qos.logback.core.read.ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            noticeRunner()
                    .withPropertyValues("SLACK_NOTICE_SUMMARY_ENABLED=true")
                    .run(
                            context -> {
                                assertThat(context).hasNotFailed();
                                assertThat(context.getBean(NoticeSummarizerPort.class))
                                        .isNotInstanceOf(GeminiNoticeSummarizerAdapter.class);
                            });
            assertThat(appender.list)
                    .anyMatch(
                            e ->
                                    e.getLevel() == ch.qos.logback.classic.Level.WARN
                                            && e.getFormattedMessage().contains("GEMINI_API_KEY"));
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    @DisplayName("요약을 켜고 키를 주면 Gemini 요약기가 만들어지고, 기본 모델은 gemini-3.8-flash 이며 toString 에 키가 없다")
    void summaryEnabledWithKey() {
        noticeRunner()
                .withPropertyValues(
                        "SLACK_NOTICE_SUMMARY_ENABLED=true", "GEMINI_API_KEY=test-key-do-not-leak")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            NoticeSummarizerPort port = context.getBean(NoticeSummarizerPort.class);
                            assertThat(port).isInstanceOf(GeminiNoticeSummarizerAdapter.class);
                            assertThat(port.toString())
                                    .contains("gemini-3.8-flash")
                                    .doesNotContain("test-key-do-not-leak");
                            Object model =
                                    org.springframework.test.util.ReflectionTestUtils.getField(
                                            port, "model");
                            Object timeout =
                                    org.springframework.test.util.ReflectionTestUtils.getField(
                                            port, "timeout");
                            Object thinking =
                                    org.springframework.test.util.ReflectionTestUtils.getField(
                                            port, "thinkingLevel");
                            assertThat(model).isEqualTo("gemini-3.8-flash");
                            assertThat(timeout).isEqualTo(java.time.Duration.ofSeconds(8));
                            assertThat(thinking).isEqualTo("low");
                        });
    }

    @Test
    @DisplayName("모델·제한 시간은 환경변수로 바꿀 수 있다")
    void summaryModelAndTimeoutConfigurable() {
        noticeRunner()
                .withPropertyValues(
                        "SLACK_NOTICE_SUMMARY_ENABLED=true",
                        "GEMINI_API_KEY=k",
                        "SLACK_NOTICE_SUMMARY_MODEL=gemini-custom",
                        "SLACK_NOTICE_SUMMARY_TIMEOUT=3s")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            NoticeSummarizerPort port = context.getBean(NoticeSummarizerPort.class);
                            assertThat(
                                            org.springframework.test.util.ReflectionTestUtils
                                                    .getField(port, "model"))
                                    .isEqualTo("gemini-custom");
                            assertThat(
                                            org.springframework.test.util.ReflectionTestUtils
                                                    .getField(port, "timeout"))
                                    .isEqualTo(java.time.Duration.ofSeconds(3));
                        });
    }

    @Test
    @DisplayName("모델 이름 형식이 이상하면 부팅이 실패하고 실패 메시지에 키가 나오지 않는다")
    void invalidModelFailsStartupWithoutLeakingKey() {
        noticeRunner()
                .withPropertyValues(
                        "SLACK_NOTICE_SUMMARY_ENABLED=true",
                        "GEMINI_API_KEY=test-key-do-not-leak",
                        "SLACK_NOTICE_SUMMARY_MODEL=bad/model?x=1")
                .run(
                        context -> {
                            assertThat(context).hasFailed();
                            assertThat(rootMessages(context.getStartupFailure()))
                                    .doesNotContain("test-key-do-not-leak");
                        });
    }
}
