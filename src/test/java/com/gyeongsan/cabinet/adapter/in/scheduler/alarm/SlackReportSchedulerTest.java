package com.gyeongsan.cabinet.adapter.in.scheduler.alarm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.gyeongsan.cabinet.domain.alarm.port.in.ForwardSlackReportsUseCase;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class SlackReportSchedulerTest {

    @Test
    @DisplayName("주기마다 전달 유스케이스를 호출한다")
    void polls() {
        ForwardSlackReportsUseCase useCase = mock(ForwardSlackReportsUseCase.class);

        new SlackReportScheduler(useCase).poll();

        verify(useCase).forwardNewReports();
    }

    @Test
    @DisplayName("예상치 못한 예외가 나도 스케줄러 스레드로 퍼지지 않는다")
    void swallowsUnexpectedFailures() {
        ForwardSlackReportsUseCase useCase = mock(ForwardSlackReportsUseCase.class);
        doThrow(new IllegalStateException("boom")).when(useCase).forwardNewReports();

        assertThatCode(() -> new SlackReportScheduler(useCase).poll()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("겹쳐 실행되지 않도록 고정 지연 방식이고, 여러 서버에서는 ShedLock 으로 한 곳만 실행한다")
    void scheduledWithFixedDelayAndLock() throws Exception {
        var method = SlackReportScheduler.class.getMethod("poll");

        Scheduled scheduled = method.getAnnotation(Scheduled.class);
        assertThat(scheduled.fixedDelayString()).contains("poll-interval-ms");
        assertThat(scheduled.fixedRateString()).isEmpty();
        assertThat(method.getAnnotation(SchedulerLock.class)).isNotNull();
    }
}
