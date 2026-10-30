package com.gyeongsan.cabinet.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RedisStreamConfigTest {

    @Test
    @DisplayName("스트림 읽기 에러가 나면 바로 재시도하지 않고 기다린다")
    void readError_backsOffBeforeRetry() {
        long start = System.nanoTime();

        RedisStreamConfig.backOffOnReadError(new IllegalStateException("redis down"));

        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
        assertThat(elapsedMillis)
                .isGreaterThanOrEqualTo(RedisStreamConfig.READ_ERROR_BACKOFF.toMillis());
    }

    @Test
    @DisplayName("기다리는 중 인터럽트되면 인터럽트 상태를 유지하고 바로 끝낸다")
    void readError_interrupted_keepsInterruptFlag() {
        Thread.currentThread().interrupt();

        RedisStreamConfig.backOffOnReadError(new IllegalStateException("redis down"));

        assertThat(Thread.interrupted()).isTrue();
    }
}
