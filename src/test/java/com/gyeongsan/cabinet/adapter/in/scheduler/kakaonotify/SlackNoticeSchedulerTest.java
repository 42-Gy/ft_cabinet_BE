package com.gyeongsan.cabinet.adapter.in.scheduler.kakaonotify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.gyeongsan.cabinet.domain.kakaonotify.port.in.ForwardSlackNoticesUseCase;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class SlackNoticeSchedulerTest {

    @Test
    @DisplayName("주기마다 전달 유스케이스를 호출한다")
    void polls() {
        ForwardSlackNoticesUseCase useCase = mock(ForwardSlackNoticesUseCase.class);

        new SlackNoticeScheduler(useCase).poll();

        verify(useCase).forwardNewNotices();
    }

    @Test
    @DisplayName("예상치 못한 예외가 나도 스케줄러 스레드로 퍼지지 않는다")
    void swallowsUnexpectedFailures() {
        ForwardSlackNoticesUseCase useCase = mock(ForwardSlackNoticesUseCase.class);
        doThrow(new IllegalStateException("boom")).when(useCase).forwardNewNotices();

        assertThatCode(() -> new SlackNoticeScheduler(useCase).poll()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("고정 지연 방식이고, 여러 서버에서는 ShedLock 으로 한 곳만 실행한다(오류제보와 다른 락 이름)")
    void scheduledWithFixedDelayAndLock() throws Exception {
        var method = SlackNoticeScheduler.class.getMethod("poll");

        Scheduled scheduled = method.getAnnotation(Scheduled.class);
        assertThat(scheduled.fixedDelayString()).contains("poll-interval-ms");
        assertThat(scheduled.fixedRateString()).isEmpty();
        SchedulerLock lock = method.getAnnotation(SchedulerLock.class);
        assertThat(lock).isNotNull();
        assertThat(lock.name()).isEqualTo("slackNoticeForwardTask");
    }
}
