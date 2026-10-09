package com.gyeongsan.cabinet.application.chatbot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.gyeongsan.cabinet.adapter.out.embedding.OnnxEmbeddingAdapter;
import com.gyeongsan.cabinet.adapter.out.metrics.ProcessRssMetrics;
import com.gyeongsan.cabinet.application.chatbot.ChatbotEvalSupport.EvalSet;
import com.gyeongsan.cabinet.application.chatbot.ChatbotEvalSupport.SeedFaq;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 임베딩 모델의 메모리와 지연을 잰다(리포트 전용, 기준을 두지 않는다).
 *
 * <p>측정 항목: 모델 로드 시간, 로드 전후 프로세스 RSS(ONNX 는 힙이 아니라 네이티브 메모리를 쓴다), 첫 질문(콜드) 지연, 질문 지연 p50/p95/p99,
 * 동시 2개/4개 요청 때의 처리량과 지연, 색인 전체(FAQ 질문 표현 전부)를 임베딩하는 시간.
 *
 * <p><b>이 값은 Azure 테스트 서버(B2)의 실측이 아니다.</b> GitHub Actions 러너에서 CPU 를 2코어로 제한(taskset -c 0,1)하고 같은
 * 설정(스레드 2)으로 돌린 참고치다. CPU 세대와 클럭이 달라 지연은 서버와 다를 수 있다. 서버 실측은 챗봇을 켠 뒤 /actuator/metrics 의
 * chatbot.process.rss.bytes, chatbot.ask.duration 으로 한다.
 *
 * <p>환경변수 CHATBOT_EVAL_MODELS_DIR 가 있어야 실행된다(없으면 건너뜀). 결과는 build/chatbot-eval/footprint.md.
 */
class ChatbotFootprintTest {

    private static final int WARM_RUNS = 200;
    private static final int PER_THREAD = 60;

    private static double ms(long nanos) {
        return nanos / 1_000_000.0;
    }

    private static long percentile(long[] sorted, double q) {
        int idx = (int) Math.min(sorted.length - 1, Math.round(q * (sorted.length - 1)));
        return sorted[idx];
    }

    private static String mb(OptionalLong kb) {
        return kb.isPresent()
                ? String.format(Locale.ROOT, "%.0f MB", kb.getAsLong() / 1024.0)
                : "—";
    }

    private static String heapMb() {
        Runtime rt = Runtime.getRuntime();
        return String.format(
                Locale.ROOT, "%.0f MB", (rt.totalMemory() - rt.freeMemory()) / 1048576.0);
    }

    record Run(double wallMs, long[] latencies) {}

    private static Run concurrent(OnnxEmbeddingAdapter adapter, List<String> texts, int threads)
            throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<long[]>> tasks = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                final int offset = t * 7;
                tasks.add(
                        () -> {
                            long[] lat = new long[PER_THREAD];
                            for (int i = 0; i < PER_THREAD; i++) {
                                String text = texts.get((offset + i) % texts.size());
                                long s = System.nanoTime();
                                adapter.embed(text);
                                lat[i] = System.nanoTime() - s;
                            }
                            return lat;
                        });
            }
            long start = System.nanoTime();
            List<Future<long[]>> futures = pool.invokeAll(tasks);
            double wall = ms(System.nanoTime() - start);
            long[] all = new long[threads * PER_THREAD];
            int pos = 0;
            for (Future<long[]> f : futures) {
                long[] lat = f.get();
                System.arraycopy(lat, 0, all, pos, lat.length);
                pos += lat.length;
            }
            Arrays.sort(all);
            return new Run(wall, all);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("임베딩 모델의 메모리와 지연을 잰다(참고치, 리포트 전용)")
    void measure() throws Exception {
        String root = System.getenv("CHATBOT_EVAL_MODELS_DIR");
        assumeTrue(root != null && !root.isBlank(), "CHATBOT_EVAL_MODELS_DIR 가 없어 건너뜁니다");
        List<Path> dirs;
        try (Stream<Path> stream = Files.list(Path.of(root))) {
            dirs =
                    stream.filter(Files::isDirectory)
                            .filter(d -> Files.exists(d.resolve("model.onnx")))
                            .filter(d -> Files.exists(d.resolve("tokenizer.json")))
                            .sorted()
                            .toList();
        }
        assumeTrue(!dirs.isEmpty(), "model.onnx 가 있는 모델 디렉터리가 없어 건너뜁니다");

        List<SeedFaq> faqs = ChatbotEvalSupport.loadFaqs();
        List<String> variants = faqs.stream().flatMap(f -> f.questions().stream()).toList();
        EvalSet dev = ChatbotEvalSupport.loadEvalSet("/chatbot/eval-set.json");
        EvalSet holdout = ChatbotEvalSupport.loadEvalSet("/chatbot/eval-holdout.json");
        List<String> questions = new ArrayList<>();
        dev.inScope().forEach(i -> questions.add(TextNormalizer.normalize(i.question())));
        holdout.inScope().forEach(i -> questions.add(TextNormalizer.normalize(i.question())));
        questions.addAll(dev.outOfScope());
        questions.addAll(holdout.outOfScope());

        StringBuilder report = new StringBuilder("## 챗봇 임베딩 메모리·지연(CI 러너 참고치)\n\n");
        report.append(
                String.format(
                        Locale.ROOT,
                        "> Azure 테스트 서버 실측이 **아닙니다**. 러너 CPU 를 2코어로 제한해(보이는 코어 수: %d) 같은 설정(ONNX 스레드 2)으로 돌린 참고치이며, CPU 세대·클럭이 서버와 다릅니다. 서버 값은 챗봇을 켠 뒤 `/actuator/metrics`(`chatbot.process.rss.bytes`, `chatbot.ask.duration`)로 확인하세요.%n%n",
                        Runtime.getRuntime().availableProcessors()));

        for (Path dir : dirs) {
            String id = dir.getFileName().toString();
            Properties props = new Properties();
            if (Files.exists(dir.resolve("model.properties"))) {
                try (var r =
                        Files.newBufferedReader(
                                dir.resolve("model.properties"), StandardCharsets.UTF_8)) {
                    props.load(r);
                }
            }
            long modelBytes = Files.size(dir.resolve("model.onnx"));
            // 힙 정리가 RSS 를 흔들지 않게 기준값을 잡기 전에 한 번 정리한다(그래도 RSS 는 몇십 MB 흔들릴 수 있다).
            System.gc();
            OptionalLong rssBefore = ProcessRssMetrics.currentRssKb();
            String heapBefore = heapMb();

            long loadStart = System.nanoTime();
            try (OnnxEmbeddingAdapter adapter =
                    new OnnxEmbeddingAdapter(
                            id,
                            dir.resolve("model.onnx"),
                            dir.resolve("tokenizer.json"),
                            props.getProperty("prefix", ""),
                            Integer.parseInt(props.getProperty("maxTokens", "128")),
                            2)) {
                double loadMs = ms(System.nanoTime() - loadStart);
                OptionalLong rssLoaded = ProcessRssMetrics.currentRssKb();

                long coldStart = System.nanoTime();
                adapter.embed("준비 확인");
                double coldMs = ms(System.nanoTime() - coldStart);

                // 색인 전체를 임베딩하는 시간(시작할 때와 FAQ 를 고칠 때마다 한 번 일어난다)
                long indexStart = System.nanoTime();
                for (String v : variants) {
                    adapter.embed(TextNormalizer.normalize(v));
                }
                double indexMs = ms(System.nanoTime() - indexStart);

                // 순차 질문 지연
                long[] warm = new long[WARM_RUNS];
                for (int i = 0; i < WARM_RUNS; i++) {
                    long s = System.nanoTime();
                    adapter.embed(questions.get(i % questions.size()));
                    warm[i] = System.nanoTime() - s;
                }
                Arrays.sort(warm);
                OptionalLong rssAfter = ProcessRssMetrics.currentRssKb();

                Run two = concurrent(adapter, questions, 2);
                Run four = concurrent(adapter, questions, 4);
                OptionalLong peak = ProcessRssMetrics.peakRssKb();

                report.append(String.format(Locale.ROOT, "### %s%n%n", id));
                report.append("| 항목 | 값 |\n|---|---|\n");
                report.append(
                        String.format(
                                Locale.ROOT, "| 모델 파일 | %.0f MB |%n", modelBytes / 1048576.0));
                report.append(String.format(Locale.ROOT, "| 모델 로드 시간 | %.0f ms |%n", loadMs));
                report.append(
                        String.format(
                                Locale.ROOT,
                                "| RSS 로드 전 → 로드 직후 → 질문 처리 후 → 최고 | %s → %s → %s → %s |%n",
                                mb(rssBefore),
                                mb(rssLoaded),
                                mb(rssAfter),
                                mb(peak)));
                report.append(
                        String.format(
                                Locale.ROOT,
                                "| 모델이 늘린 RSS(로드 직후 − 로드 전) | %s |%n",
                                rssBefore.isPresent() && rssLoaded.isPresent()
                                        ? String.format(
                                                Locale.ROOT,
                                                "%.0f MB",
                                                (rssLoaded.getAsLong() - rssBefore.getAsLong())
                                                        / 1024.0)
                                        : "—"));
                report.append(
                        String.format(
                                Locale.ROOT,
                                "| JVM 힙 사용(로드 전 → 후) | %s → %s |%n",
                                heapBefore,
                                heapMb()));
                report.append(String.format(Locale.ROOT, "| 첫 질문(콜드) 지연 | %.1f ms |%n", coldMs));
                report.append(
                        String.format(
                                Locale.ROOT,
                                "| 색인 전체 임베딩(질문 표현 %d개) | %.0f ms |%n",
                                variants.size(),
                                indexMs));
                report.append(
                        String.format(
                                Locale.ROOT,
                                "| 질문 1개(순차) p50 / p95 / p99 / 최대 | %.1f / %.1f / %.1f / %.1f ms |%n",
                                ms(percentile(warm, 0.5)),
                                ms(percentile(warm, 0.95)),
                                ms(percentile(warm, 0.99)),
                                ms(warm[warm.length - 1])));
                report.append(
                        String.format(
                                Locale.ROOT,
                                "| 동시 2개 요청: 처리량 / p50 / p95 | %.0f 건/s / %.1f / %.1f ms |%n",
                                two.latencies().length / (two.wallMs() / 1000.0),
                                ms(percentile(two.latencies(), 0.5)),
                                ms(percentile(two.latencies(), 0.95))));
                report.append(
                        String.format(
                                Locale.ROOT,
                                "| 동시 4개 요청: 처리량 / p50 / p95 | %.0f 건/s / %.1f / %.1f ms |%n",
                                four.latencies().length / (four.wallMs() / 1000.0),
                                ms(percentile(four.latencies(), 0.5)),
                                ms(percentile(four.latencies(), 0.95))));
                report.append(
                        "\n동시 4개 행은 서비스의 동시 임베딩 제한(기본 2개, 넘으면 2초 대기 후 503)을 넘는 상황의 지연을 보려는 것입니다.\n\n");

                assertThat(adapter.embed("확인")).isNotEmpty();
            }
        }

        Path out = Path.of("build/chatbot-eval/footprint.md");
        Files.createDirectories(out.getParent());
        Files.writeString(out, report.toString(), StandardCharsets.UTF_8);
        System.out.println(report);
    }
}
