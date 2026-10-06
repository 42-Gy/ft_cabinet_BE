package com.gyeongsan.cabinet.adapter.in.scheduler.alarm;

import com.gyeongsan.cabinet.domain.alarm.port.in.ForwardSlackReportsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 일정 간격으로 오류제보 채널을 확인한다. 서버가 여러 대여도 ShedLock 으로 한 곳에서만 실행된다. */
@Component
@ConditionalOnProperty(name = "app.slack-report.enabled", havingValue = "true")
@RequiredArgsConstructor
@Log4j2
public class SlackReportScheduler {

    private final ForwardSlackReportsUseCase forwardSlackReports;

    @Scheduled(
            initialDelayString = "${app.slack-report.initial-delay-ms:15000}",
            fixedDelayString = "${app.slack-report.poll-interval-ms:60000}")
    @SchedulerLock(
            name = "slackReportForwardTask",
            lockAtMostFor = "PT5M",
            lockAtLeastFor = "PT10S")
    public void poll() {
        try {
            forwardSlackReports.forwardNewReports();
        } catch (RuntimeException e) {
            // 스케줄러 스레드가 예외로 죽지 않게 하고, 다음 주기에 다시 시도한다.
            log.error("[SlackReport] 전달 중 예상치 못한 오류: {}", e.toString(), e);
        }
    }
}
