package com.gyeongsan.cabinet.adapter.in.scheduler.kakaonotify;

import com.gyeongsan.cabinet.domain.kakaonotify.port.in.ForwardSlackNoticesUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 일정 간격으로 공지 채널을 확인한다. 서버가 여러 대여도 ShedLock 으로 한 곳에서만 실행된다. 수신자가 많으면 한 번의 실행이 길어질 수 있어 락 최대 시간을
 * 오류제보보다 길게(10분) 둔다.
 */
@Component
@ConditionalOnProperty(name = "app.slack-notice.enabled", havingValue = "true")
@RequiredArgsConstructor
@Log4j2
public class SlackNoticeScheduler {

    private final ForwardSlackNoticesUseCase forwardSlackNotices;

    @Scheduled(
            initialDelayString = "${app.slack-notice.initial-delay-ms:20000}",
            fixedDelayString = "${app.slack-notice.poll-interval-ms:60000}")
    @SchedulerLock(
            name = "slackNoticeForwardTask",
            lockAtMostFor = "PT10M",
            lockAtLeastFor = "PT10S")
    public void poll() {
        try {
            forwardSlackNotices.forwardNewNotices();
        } catch (RuntimeException e) {
            // 스케줄러 스레드가 예외로 죽지 않게 하고, 다음 주기에 다시 시도한다.
            log.error("[SlackNotice] 전달 중 예상치 못한 오류: {}", e.toString(), e);
        }
    }
}
