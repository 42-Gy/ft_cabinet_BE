package com.gyeongsan.cabinet.adapter.out.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalLong;

/**
 * 프로세스가 실제로 쓰는 메모리(RSS, 바이트)를 지표로 노출한다. ONNX 모델은 JVM 힙이 아니라 네이티브 메모리를 쓰기 때문에 jvm.memory.* 지표로는 모델이
 * 늘린 메모리가 보이지 않는다. 리눅스(/proc)에서만 값이 나오고, 읽을 수 없으면 지표를 만들지 않는다.
 */
public class ProcessRssMetrics implements MeterBinder {

    private final Path statusFile;

    public ProcessRssMetrics() {
        this(Path.of("/proc/self/status"));
    }

    ProcessRssMetrics(Path statusFile) {
        this.statusFile = statusFile;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        if (readKb("VmRSS:").isEmpty()) {
            return;
        }
        Gauge.builder("chatbot.process.rss.bytes", this, m -> m.readKb("VmRSS:").orElse(0) * 1024.0)
                .description("프로세스 상주 메모리(RSS). 네이티브 메모리(ONNX 모델)를 포함한다")
                .baseUnit("bytes")
                .register(registry);
        Gauge.builder(
                        "chatbot.process.rss.peak.bytes",
                        this,
                        m -> m.readKb("VmHWM:").orElse(0) * 1024.0)
                .description("프로세스 상주 메모리의 최고치(VmHWM)")
                .baseUnit("bytes")
                .register(registry);
    }

    /** "VmRSS: 123456 kB" 같은 줄에서 kB 값을 읽는다. */
    OptionalLong readKb(String key) {
        try {
            for (String line : Files.readAllLines(statusFile, StandardCharsets.UTF_8)) {
                if (line.startsWith(key)) {
                    String digits = line.substring(key.length()).replaceAll("[^0-9]", "");
                    return digits.isEmpty()
                            ? OptionalLong.empty()
                            : OptionalLong.of(Long.parseLong(digits));
                }
            }
        } catch (IOException | RuntimeException e) {
            return OptionalLong.empty();
        }
        return OptionalLong.empty();
    }

    /** 테스트와 평가 도구가 같은 방식으로 읽을 수 있게 현재 프로세스의 값을 돌려준다. */
    public static OptionalLong currentRssKb() {
        return new ProcessRssMetrics().readKb("VmRSS:");
    }

    public static OptionalLong peakRssKb() {
        return new ProcessRssMetrics().readKb("VmHWM:");
    }
}
