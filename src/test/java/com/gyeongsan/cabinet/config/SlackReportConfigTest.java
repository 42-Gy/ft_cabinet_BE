package com.gyeongsan.cabinet.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.gyeongsan.cabinet.application.alarm.SlackReportSettings;
import com.gyeongsan.cabinet.domain.alarm.port.in.ForwardSlackReportsUseCase;
import com.gyeongsan.cabinet.domain.alarm.port.out.AlarmPort;
import com.gyeongsan.cabinet.domain.alarm.port.out.ReportCursorPort;
import com.gyeongsan.cabinet.domain.alarm.port.out.ReportRecipientPort;
import com.gyeongsan.cabinet.domain.alarm.port.out.SlackChannelPort;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

class SlackReportConfigTest {

    private static final Path MAIN_YML = Path.of("src/main/resources/application.yml");

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withUserConfiguration(SlackReportConfig.class)
                .withBean(SlackChannelPort.class, () -> mock(SlackChannelPort.class))
                .withBean(ReportCursorPort.class, () -> mock(ReportCursorPort.class))
                .withBean(AlarmPort.class, () -> mock(AlarmPort.class))
                .withBean(ReportRecipientPort.class, () -> mock(ReportRecipientPort.class))
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
    @DisplayName("운영 application.yml 기본값에서는 꺼져 있어 아무 빈도 만들어지지 않는다")
    void disabledByDefault() {
        runner().run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context).doesNotHaveBean(ForwardSlackReportsUseCase.class);
                            assertThat(context).doesNotHaveBean(SlackReportSettings.class);
                        });
    }

    @Test
    @DisplayName("켜면서 채널 ID 만 주면 켜진다 (수신자는 설정이 아니라 DB 에서 읽으므로 수신자 설정은 필요 없다)")
    void enabledWithChannelIdOnly() {
        runner().withPropertyValues(
                        "SLACK_REPORT_FORWARD_ENABLED=true", "SLACK_REPORT_CHANNEL_ID= C0REPORT ")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context).hasSingleBean(ForwardSlackReportsUseCase.class);
                            SlackReportSettings settings =
                                    context.getBean(SlackReportSettings.class);
                            assertThat(settings.channelId()).isEqualTo("C0REPORT");
                            assertThat(settings.maxPerPoll()).isEqualTo(20);
                        });
    }

    @Test
    @DisplayName("예전 SLACK_REPORT_RECIPIENTS 가 남아 있어도 무시되고 부팅에는 영향이 없다")
    void legacyRecipientsSettingIsIgnored() {
        runner().withPropertyValues(
                        "SLACK_REPORT_FORWARD_ENABLED=true",
                        "SLACK_REPORT_CHANNEL_ID=C0REPORT",
                        "SLACK_REPORT_RECIPIENTS=admin1,admin2")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    @DisplayName("켰는데 채널 ID 가 없으면 부팅이 실패한다 (조용히 엉뚱한 곳을 읽지 않는다)")
    void enabledWithoutChannelIdFailsStartup() {
        runner().withPropertyValues("SLACK_REPORT_FORWARD_ENABLED=true")
                .run(context -> assertThat(context).hasFailed());
    }
}
