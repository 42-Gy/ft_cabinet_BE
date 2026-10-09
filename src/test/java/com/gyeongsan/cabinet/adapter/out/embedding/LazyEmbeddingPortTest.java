package com.gyeongsan.cabinet.adapter.out.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gyeongsan.cabinet.domain.chatbot.model.EmbeddingUnavailableException;
import com.gyeongsan.cabinet.domain.chatbot.port.out.EmbeddingPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LazyEmbeddingPortTest {

    /** 직접 움직일 수 있는 시계. */
    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-10-07T00:00:00Z");

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private static EmbeddingPort stub() {
        return new EmbeddingPort() {
            @Override
            public float[] embed(String text) {
                return new float[] {1f};
            }

            @Override
            public String modelId() {
                return "stub";
            }
        };
    }

    @Test
    @DisplayName("처음 쓸 때 한 번만 불러온다 (부팅 때는 불러오지 않는다)")
    void loadsLazilyOnce() {
        AtomicInteger loads = new AtomicInteger();
        LazyEmbeddingPort port =
                new LazyEmbeddingPort(
                        "m",
                        () -> {
                            loads.incrementAndGet();
                            return stub();
                        },
                        new MutableClock(),
                        Duration.ofSeconds(60));
        assertThat(loads).hasValue(0);

        port.embed("a");
        port.embed("b");

        assertThat(loads).hasValue(1);
        assertThat(port.modelId()).isEqualTo("m");
    }

    @Test
    @DisplayName("불러오기에 실패하면 EmbeddingUnavailableException 이고, 대기 시간 동안은 다시 시도하지 않는다")
    void backsOffAfterFailure() {
        MutableClock clock = new MutableClock();
        AtomicInteger attempts = new AtomicInteger();
        LazyEmbeddingPort port =
                new LazyEmbeddingPort(
                        "m",
                        () -> {
                            attempts.incrementAndGet();
                            throw new IllegalStateException("파일 없음");
                        },
                        clock,
                        Duration.ofSeconds(60));

        assertThatThrownBy(() -> port.embed("a")).isInstanceOf(EmbeddingUnavailableException.class);
        assertThatThrownBy(() -> port.embed("a")).isInstanceOf(EmbeddingUnavailableException.class);
        assertThat(attempts).hasValue(1);

        clock.now = clock.now.plusSeconds(61);
        assertThatThrownBy(() -> port.embed("a")).isInstanceOf(EmbeddingUnavailableException.class);
        assertThat(attempts).hasValue(2);
    }

    @Test
    @DisplayName("대기 후 불러오기에 성공하면 정상 동작한다")
    void recoversAfterBackoff() {
        MutableClock clock = new MutableClock();
        AtomicInteger attempts = new AtomicInteger();
        LazyEmbeddingPort port =
                new LazyEmbeddingPort(
                        "m",
                        () -> {
                            if (attempts.incrementAndGet() == 1) {
                                throw new EmbeddingUnavailableException("처음엔 없음");
                            }
                            return stub();
                        },
                        clock,
                        Duration.ofSeconds(60));

        assertThatThrownBy(() -> port.embed("a")).isInstanceOf(EmbeddingUnavailableException.class);
        clock.now = clock.now.plusSeconds(61);

        assertThat(port.embed("a")).containsExactly(1f);
    }

    @Test
    @DisplayName("네이티브 라이브러리 로딩 실패(LinkageError)도 서버를 죽이지 않고 '사용 불가'로 바꾼다")
    void linkageErrorIsContained() {
        LazyEmbeddingPort port =
                new LazyEmbeddingPort(
                        "m",
                        () -> {
                            throw new UnsatisfiedLinkError("libonnxruntime.so");
                        },
                        new MutableClock(),
                        Duration.ofSeconds(60));

        assertThatThrownBy(() -> port.embed("a"))
                .isInstanceOf(EmbeddingUnavailableException.class)
                .hasMessageContaining("libonnxruntime");
    }
}
