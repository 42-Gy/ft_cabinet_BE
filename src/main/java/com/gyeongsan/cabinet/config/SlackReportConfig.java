package com.gyeongsan.cabinet.config;

import com.gyeongsan.cabinet.application.alarm.SlackReportForwardService;
import com.gyeongsan.cabinet.application.alarm.SlackReportSettings;
import com.gyeongsan.cabinet.domain.alarm.port.in.ForwardSlackReportsUseCase;
import com.gyeongsan.cabinet.domain.alarm.port.out.AlarmPort;
import com.gyeongsan.cabinet.domain.alarm.port.out.ReportCursorPort;
import com.gyeongsan.cabinet.domain.alarm.port.out.ReportRecipientPort;
import com.gyeongsan.cabinet.domain.alarm.port.out.SlackChannelPort;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 오류제보 → 관리자 DM 전달 기능. SLACK_REPORT_FORWARD_ENABLED=true 일 때만 켜지고, 켠 상태에서 채널 ID 가 없으면 부팅이 실패한다. 설정
 * 실수로 엉뚱한 환경(특히 운영)의 채널을 조용히 읽는 일을 막기 위해 기본은 꺼 둔다.
 */
@Configuration
@ConditionalOnProperty(name = "app.slack-report.enabled", havingValue = "true")
public class SlackReportConfig {

    @Bean
    public SlackReportSettings slackReportSettings(
            @Value("${app.slack-report.channel-id:}") String channelId,
            @Value("${app.slack-report.max-per-poll:20}") int maxPerPoll,
            @Value("${app.slack-report.max-text-length:1500}") int maxTextLength) {
        return new SlackReportSettings(channelId.trim(), maxPerPoll, maxTextLength);
    }

    @Bean
    public ForwardSlackReportsUseCase forwardSlackReportsUseCase(
            SlackChannelPort channelPort,
            ReportCursorPort cursorPort,
            AlarmPort alarmPort,
            ReportRecipientPort recipientPort,
            SlackReportSettings settings) {
        return new SlackReportForwardService(
                channelPort, cursorPort, alarmPort, recipientPort, settings, Clock.systemUTC());
    }
}
