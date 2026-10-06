package com.gyeongsan.cabinet.adapter.out.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gyeongsan.cabinet.application.chatbot.ChatbotService;
import com.gyeongsan.cabinet.application.chatbot.ChatbotSettings;
import com.gyeongsan.cabinet.application.chatbot.FaqIndexManager;
import com.gyeongsan.cabinet.domain.chatbot.model.ChatbotAnswer;
import com.gyeongsan.cabinet.domain.chatbot.model.Faq;
import com.gyeongsan.cabinet.domain.chatbot.port.out.ChatbotMetricsPort;
import com.gyeongsan.cabinet.domain.chatbot.port.out.EmbeddingPort;
import com.gyeongsan.cabinet.domain.chatbot.port.out.FaqRepositoryPort;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ChatbotMetricsTest {

    @Test
    @DisplayName("질문 처리 시간이 chatbot.ask.duration 타이머에 쌓이고 질문 내용은 태그에 없다")
    void askDurationTimer() {
        MeterRegistry registry = new SimpleMeterRegistry();
        MicrometerChatbotMetricsAdapter adapter = new MicrometerChatbotMetricsAdapter(registry);

        adapter.recordAskDuration(TimeUnit.MILLISECONDS.toNanos(40));
        adapter.recordAskDuration(TimeUnit.MILLISECONDS.toNanos(60));

        var timer = registry.get("chatbot.ask.duration").timer();
        assertThat(timer.count()).isEqualTo(2);
        assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(100.0);
        assertThat(timer.getId().getTags()).isEmpty();
    }

    @Test
    @DisplayName("/proc/self/status 형식에서 RSS 와 최고치를 읽어 게이지로 노출한다")
    void rssGauges(@TempDir Path dir) throws IOException {
        Path status = dir.resolve("status");
        Files.writeString(
                status, "Name:\tjava\nVmHWM:\t  2048000 kB\nVmRSS:\t  1024000 kB\nThreads:\t40\n");
        MeterRegistry registry = new SimpleMeterRegistry();

        new ProcessRssMetrics(status).bindTo(registry);

        assertThat(registry.get("chatbot.process.rss.bytes").gauge().value())
                .isEqualTo(1024000.0 * 1024);
        assertThat(registry.get("chatbot.process.rss.peak.bytes").gauge().value())
                .isEqualTo(2048000.0 * 1024);
    }

    @Test
    @DisplayName("읽을 수 없는 환경(리눅스가 아니거나 형식이 다름)에서는 지표를 만들지 않고 실패하지 않는다")
    void rssUnavailable(@TempDir Path dir) throws IOException {
        MeterRegistry registry = new SimpleMeterRegistry();
        new ProcessRssMetrics(dir.resolve("missing")).bindTo(registry);
        assertThat(registry.getMeters()).isEmpty();

        Path broken = dir.resolve("broken");
        Files.writeString(broken, "VmRSS: abc\n");
        new ProcessRssMetrics(broken).bindTo(registry);
        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    @DisplayName("서비스는 정상 처리한 질문마다 처리 시간을 한 번 기록하고, 거절한 질문은 기록하지 않는다")
    void serviceRecordsDurationOnlyForAnsweredAsks() {
        List<Long> durations = new ArrayList<>();
        ChatbotMetricsPort metrics =
                new ChatbotMetricsPort() {
                    @Override
                    public void recordAsk(ChatbotAnswer.Result result) {}

                    @Override
                    public void recordAskDuration(long nanos) {
                        durations.add(nanos);
                    }
                };
        EmbeddingPort embedding =
                new EmbeddingPort() {
                    @Override
                    public float[] embed(String text) {
                        return new float[] {1f, 0f};
                    }

                    @Override
                    public String modelId() {
                        return "fake";
                    }
                };
        FaqIndexManager manager = new FaqIndexManager(new OneFaqRepository(), embedding);
        manager.rebuild();
        ChatbotService service =
                new ChatbotService(
                        manager,
                        embedding,
                        metrics,
                        new ChatbotSettings(0.9, 0.8, 3, 0.0, 2, 50, 2, "m"));

        service.ask("질문");
        assertThat(durations).hasSize(1).allSatisfy(d -> assertThat(d).isGreaterThanOrEqualTo(0));

        assertThatThrownBy(() -> service.ask("  ")).isInstanceOf(ServiceException.class);
        assertThat(durations).hasSize(1);
    }

    private static final class OneFaqRepository implements FaqRepositoryPort {
        private final Faq faq =
                new Faq(
                        1L,
                        "k",
                        "대여",
                        "답",
                        true,
                        List.of("질문"),
                        LocalDateTime.now(),
                        LocalDateTime.now());

        @Override
        public List<Faq> findAll() {
            return List.of(faq);
        }

        @Override
        public List<Faq> findAllEnabled() {
            return List.of(faq);
        }

        @Override
        public Optional<Faq> findById(long id) {
            return Optional.of(faq);
        }

        @Override
        public Faq save(Faq f) {
            return f;
        }

        @Override
        public boolean deleteById(long id) {
            return false;
        }

        @Override
        public long count() {
            return 1;
        }

        @Override
        public String fingerprint() {
            return "1";
        }
    }
}
