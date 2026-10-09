package com.gyeongsan.cabinet.application.chatbot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.gyeongsan.cabinet.adapter.out.embedding.CharNgramEmbeddingAdapter;
import com.gyeongsan.cabinet.adapter.out.embedding.OnnxEmbeddingAdapter;
import com.gyeongsan.cabinet.application.chatbot.ChatbotEvalSupport.CachingEmbedder;
import com.gyeongsan.cabinet.application.chatbot.ChatbotEvalSupport.EvalSet;
import com.gyeongsan.cabinet.application.chatbot.ChatbotEvalSupport.SeedFaq;
import com.gyeongsan.cabinet.application.chatbot.ChatbotTestSupport.CountingMetrics;
import com.gyeongsan.cabinet.application.chatbot.ChatbotTestSupport.FakeFaqRepository;
import com.gyeongsan.cabinet.config.ChatbotPersonalConfig;
import com.gyeongsan.cabinet.domain.chatbot.model.Faq;
import com.gyeongsan.cabinet.domain.chatbot.model.PersonalIntent;
import com.gyeongsan.cabinet.domain.chatbot.port.out.EmbeddingPort;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * "내 정보" 칩 라우팅 품질 평가(보고용). 실제 운영 규칙(칩 후보 하한 = suggest-threshold, 그리고 가장 비슷한 FAQ 점수 이상)을 그대로 쓰는
 * {@link ChatbotService} 로 질문을 물어, 두 가지를 잰다.
 *
 * <ul>
 *   <li>도달률: 개인화 질문(eval-personal.json)에 올바른 인텐트의 칩이 붙는 비율, 잘못된 칩이 붙는 비율
 *   <li>오탐률: 일반 FAQ·범위 밖 질문(eval-set, eval-holdout)에 칩이 붙는 비율. 붙은 질문은 전부 나열해 사람이 판단한다(예: "패널티 언제
 *       풀려요?" 는 칩이 붙는 편이 오히려 맞다)
 * </ul>
 *
 * <p>칩은 제안일 뿐이고 개인 정보는 사용자가 누르는 별도 API 에서만 나가므로, 이 수치는 안전이 아니라 <b>사용성</b> 지표다. 평가 질문은 인텐트
 * 표현(personal-intents.json)과 따로 한 번 쓴 것이며 결과를 본 뒤 고치지 않는다. 같은 저자의 질문이라 실제 분포에서는 수치가 더 낮을 수 있다. 기준은
 * 정하지 않고 리포트만 남긴다(첫 실측 후 정한다).
 *
 * <p>환경변수 CHATBOT_EVAL_MODELS_DIR 이 있으면 그 아래 모델마다 평가하고, 없으면 글자 n-gram 기준선만 돌린다. 결과는
 * build/chatbot-eval/personal.md.
 */
class ChatbotPersonalEvaluationTest {

    record PersonalPositive(String intent, String question) {}

    record PersonalEvalSet(List<PersonalPositive> positives, List<String> thirdPerson) {}

    private static final ChatbotSettings SETTINGS =
            new ChatbotSettings(0.93, 0.88, 3, 0.02, 2, 200, 2, "못 찾았어요");

    private record Candidate(String id, EmbeddingPort port) {}

    private static List<Candidate> candidates() throws IOException {
        List<Candidate> list = new ArrayList<>();
        list.add(new Candidate("char-ngram(기준선)", new CharNgramEmbeddingAdapter()));
        String root = System.getenv("CHATBOT_EVAL_MODELS_DIR");
        if (root != null && !root.isBlank()) {
            try (Stream<Path> dirs = Files.list(Path.of(root))) {
                for (Path dir : dirs.filter(Files::isDirectory).sorted().toList()) {
                    Path model = dir.resolve("model.onnx");
                    Path tokenizer = dir.resolve("tokenizer.json");
                    if (!Files.exists(model) || !Files.exists(tokenizer)) {
                        continue;
                    }
                    Properties props = new Properties();
                    Path propsFile = dir.resolve("model.properties");
                    if (Files.exists(propsFile)) {
                        try (var reader =
                                Files.newBufferedReader(propsFile, StandardCharsets.UTF_8)) {
                            props.load(reader);
                        }
                    }
                    list.add(
                            new Candidate(
                                    dir.getFileName().toString(),
                                    new OnnxEmbeddingAdapter(
                                            dir.getFileName().toString(),
                                            model,
                                            tokenizer,
                                            props.getProperty("prefix", ""),
                                            Integer.parseInt(props.getProperty("maxTokens", "128")),
                                            2)));
                }
            }
        }
        return list;
    }

    private static ChatbotService serviceFor(EmbeddingPort port) throws IOException {
        FakeFaqRepository repository = new FakeFaqRepository();
        for (SeedFaq seed : ChatbotEvalSupport.loadFaqs()) {
            repository.save(
                    new Faq(
                            null,
                            seed.seedKey(),
                            seed.category(),
                            seed.answer(),
                            seed.enabled() == null || seed.enabled(),
                            seed.questions(),
                            null,
                            null));
        }
        CachingEmbedder cache = new CachingEmbedder(port);
        EmbeddingPort cached =
                new EmbeddingPort() {
                    @Override
                    public float[] embed(String text) {
                        return cache.embed(text).clone();
                    }

                    @Override
                    public String modelId() {
                        return port.modelId();
                    }
                };
        FaqIndexManager manager = new FaqIndexManager(repository, cached);
        manager.rebuild();
        return new ChatbotService(
                manager,
                cached,
                new CountingMetrics(),
                SETTINGS,
                new PersonalIntentRouter(ChatbotPersonalConfig.loadCatalog(), cached));
    }

    private static String pct(int n, int total) {
        return total == 0
                ? "—"
                : String.format(Locale.ROOT, "%d/%d (%.1f%%)", n, total, 100.0 * n / total);
    }

    private static String clip(String s) {
        return s.replace("|", "\\|").replace("\n", " ");
    }

    @Test
    @DisplayName("평가 질문 파일이 올바르다(모든 인텐트에 10개 이상, 인텐트 표현과 겹치지 않음)")
    void evalFileIsSound() throws IOException {
        PersonalEvalSet set =
                ChatbotEvalSupport.readResource(
                        "/chatbot/eval-personal.json", new TypeReference<PersonalEvalSet>() {});
        Map<PersonalIntent, Integer> counts = new EnumMap<>(PersonalIntent.class);
        for (PersonalPositive p : set.positives()) {
            PersonalIntent intent = PersonalIntent.fromName(p.intent()).orElseThrow();
            counts.merge(intent, 1, Integer::sum);
        }
        for (PersonalIntent intent : PersonalIntent.values()) {
            assertThat(counts.getOrDefault(intent, 0)).isGreaterThanOrEqualTo(10);
        }
        List<String> catalogQuestions =
                ChatbotPersonalConfig.loadCatalog().values().stream()
                        .flatMap(List::stream)
                        .toList();
        assertThat(set.positives().stream().map(PersonalPositive::question))
                .doesNotContainAnyElementsOf(catalogQuestions);
        assertThat(set.thirdPerson()).isNotEmpty();
    }

    @Test
    @DisplayName("칩 도달률과 오탐률을 측정해 리포트를 남긴다(기준 없음, 보고용)")
    void measure() throws IOException {
        PersonalEvalSet personal =
                ChatbotEvalSupport.readResource(
                        "/chatbot/eval-personal.json", new TypeReference<PersonalEvalSet>() {});
        EvalSet dev = ChatbotEvalSupport.loadEvalSet("/chatbot/eval-set.json");
        EvalSet holdout = ChatbotEvalSupport.loadEvalSet("/chatbot/eval-holdout.json");

        StringBuilder report = new StringBuilder("## 개인화 칩 라우팅 평가\n\n");
        report.append(
                "> 칩 규칙(운영과 같음): 인텐트 유사도 ≥ 후보 하한 "
                        + SETTINGS.suggestThreshold()
                        + " 그리고 ≥ 가장 비슷한 FAQ 점수. 칩은 제안일 뿐이고 개인 정보는 칩을 눌러 호출하는 별도 API 에서만 나가므로 "
                        + "이 수치는 안전이 아니라 사용성 지표다. 평가 질문은 같은 저자가 한 번 쓴 것이라 실제보다 높게 나올 수 있다.\n\n");

        for (Candidate candidate : candidates()) {
            ChatbotService service = serviceFor(candidate.port());
            report.append("### ").append(candidate.id()).append("\n\n");

            int correct = 0;
            int wrong = 0;
            int none = 0;
            Map<PersonalIntent, int[]> byIntent = new EnumMap<>(PersonalIntent.class);
            List<String> misses = new ArrayList<>();
            for (PersonalPositive p : personal.positives()) {
                PersonalIntent expected = PersonalIntent.fromName(p.intent()).orElseThrow();
                PersonalIntent got = service.ask(p.question()).personalAction();
                int[] counts = byIntent.computeIfAbsent(expected, k -> new int[2]);
                counts[1]++;
                if (got == expected) {
                    correct++;
                    counts[0]++;
                } else if (got == null) {
                    none++;
                    misses.add("| 없음 | " + expected + " | " + clip(p.question()) + " |");
                } else {
                    wrong++;
                    misses.add(
                            "| " + got + " (잘못) | " + expected + " | " + clip(p.question()) + " |");
                }
            }
            int total = personal.positives().size();
            report.append("**도달률(개인화 질문 → 올바른 칩)**: ").append(pct(correct, total));
            report.append(" · 칩 없음 ").append(pct(none, total));
            report.append(" · 잘못된 칩 ").append(pct(wrong, total)).append("\n\n");
            report.append("| 인텐트 | 올바른 칩 |\n|---|---|\n");
            for (PersonalIntent intent : PersonalIntent.values()) {
                int[] c = byIntent.getOrDefault(intent, new int[2]);
                report.append("| ")
                        .append(intent)
                        .append(" | ")
                        .append(pct(c[0], c[1]))
                        .append(" |\n");
            }
            report.append('\n');
            if (!misses.isEmpty()) {
                report.append("<details><summary>놓치거나 잘못 붙은 질문</summary>\n\n");
                report.append("| 결과 | 정답 | 질문 |\n|---|---|---|\n");
                misses.forEach(m -> report.append(m).append("\n"));
                report.append("\n</details>\n\n");
            }

            // 오탐: 일반 FAQ 질문과 범위 밖 질문에 칩이 붙는 경우
            List<String> general = new ArrayList<>();
            for (EvalSet set : List.of(dev, holdout)) {
                set.inScope().forEach(i -> general.add(i.question()));
                general.addAll(set.outOfScope());
            }
            List<String> falseChips = new ArrayList<>();
            for (String question : general) {
                PersonalIntent chip = service.ask(question).personalAction();
                if (chip != null) {
                    falseChips.add("| " + chip + " | " + clip(question) + " |");
                }
            }
            report.append("**오탐률(일반 FAQ·범위 밖 질문 → 칩이 붙음)**: ")
                    .append(pct(falseChips.size(), general.size()))
                    .append("\n\n");
            if (!falseChips.isEmpty()) {
                report.append(
                        "<details><summary>칩이 붙은 일반 질문(사람이 판단: 붙는 편이 맞는 것도 있음)</summary>\n\n");
                report.append("| 칩 | 질문 |\n|---|---|\n");
                falseChips.forEach(f -> report.append(f).append("\n"));
                report.append("\n</details>\n\n");
            }

            // 제3자 질문: 칩이 붙어도 보이는 것은 본인 정보뿐이다(참고용 수치)
            long thirdChips =
                    personal.thirdPerson().stream()
                            .filter(q -> service.ask(q).personalAction() != null)
                            .count();
            report.append("**제3자 질문에 칩이 붙는 수**: ")
                    .append(pct((int) thirdChips, personal.thirdPerson().size()))
                    .append(" — 붙어도 조회 대상은 항상 로그인한 본인이라 정보가 새지 않는다(구조로 보장, 별도 테스트).\n\n");

            assertThat(correct + wrong + none).isEqualTo(total);
        }

        Path out = Path.of("build/chatbot-eval/personal.md");
        Files.createDirectories(out.getParent());
        Files.writeString(out, report.toString(), StandardCharsets.UTF_8);
        System.out.println(report);
    }
}
