package com.gyeongsan.cabinet.application.chatbot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PerUserRateLimiterTest {

    private final AtomicLong nowNanos = new AtomicLong();

    private PerUserRateLimiter limiter(int limit) {
        return new PerUserRateLimiter(limit, 60_000, nowNanos::get);
    }

    private void advanceMillis(long millis) {
        nowNanos.addAndGet(millis * 1_000_000L);
    }

    @Test
    @DisplayName("한도 안에서는 허용하고 넘으면 거절한다")
    void limit() {
        PerUserRateLimiter limiter = limiter(3);
        assertThat(limiter.tryAcquire(1)).isTrue();
        assertThat(limiter.tryAcquire(1)).isTrue();
        assertThat(limiter.tryAcquire(1)).isTrue();
        assertThat(limiter.tryAcquire(1)).isFalse();
    }

    @Test
    @DisplayName("한도는 사용자별로 따로 센다(한 명이 다른 사람 몫을 쓰지 못한다)")
    void perUser() {
        PerUserRateLimiter limiter = limiter(1);
        assertThat(limiter.tryAcquire(1)).isTrue();
        assertThat(limiter.tryAcquire(1)).isFalse();
        assertThat(limiter.tryAcquire(2)).isTrue();
    }

    @Test
    @DisplayName("시간 창이 지나면 다시 허용한다")
    void windowSlides() {
        PerUserRateLimiter limiter = limiter(2);
        assertThat(limiter.tryAcquire(1)).isTrue();
        advanceMillis(30_000);
        assertThat(limiter.tryAcquire(1)).isTrue();
        assertThat(limiter.tryAcquire(1)).isFalse();
        advanceMillis(30_000); // 첫 요청이 창 밖으로 나감
        assertThat(limiter.tryAcquire(1)).isTrue();
        assertThat(limiter.tryAcquire(1)).isFalse();
    }

    @Test
    @DisplayName("거절된 요청은 기록되지 않아 창을 늘리지 않는다")
    void rejectedNotRecorded() {
        PerUserRateLimiter limiter = limiter(1);
        assertThat(limiter.tryAcquire(1)).isTrue();
        advanceMillis(59_000);
        assertThat(limiter.tryAcquire(1)).isFalse();
        advanceMillis(1_000); // 첫 요청으로부터 60초
        assertThat(limiter.tryAcquire(1)).isTrue();
    }

    @Test
    @DisplayName("지난 기록은 정리되어 메모리가 활성 사용자 수만큼만 남는다")
    void sweepsStaleUsers() {
        PerUserRateLimiter limiter = limiter(5);
        for (long user = 0; user < 2000; user++) {
            limiter.tryAcquire(user);
        }
        assertThat(limiter.trackedUsers()).isGreaterThan(1000);
        advanceMillis(120_000);
        // 정리는 호출 1024번마다 한 번 일어난다
        for (int i = 0; i < 1100; i++) {
            limiter.tryAcquire(100_000L);
        }
        assertThat(limiter.trackedUsers()).isLessThan(10);
    }

    @Test
    @DisplayName("잘못된 설정은 거부한다")
    void invalid() {
        assertThatThrownBy(() -> new PerUserRateLimiter(0, 1000))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PerUserRateLimiter(1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
