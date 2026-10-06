package com.gyeongsan.cabinet.application.chatbot;

import static org.assertj.core.api.Assertions.assertThat;

import com.gyeongsan.cabinet.adapter.out.embedding.CharNgramEmbeddingAdapter;
import com.gyeongsan.cabinet.adapter.out.embedding.OnnxEmbeddingAdapter;
import com.gyeongsan.cabinet.application.chatbot.ChatbotEvalSupport.CachingEmbedder;
import com.gyeongsan.cabinet.application.chatbot.ChatbotEvalSupport.EvalItem;
import com.gyeongsan.cabinet.application.chatbot.ChatbotEvalSupport.EvalSet;
import com.gyeongsan.cabinet.application.chatbot.ChatbotEvalSupport.Probe;
import com.gyeongsan.cabinet.application.chatbot.ChatbotEvalSupport.Row;
import com.gyeongsan.cabinet.application.chatbot.ChatbotEvalSupport.RuleCell;
import com.gyeongsan.cabinet.application.chatbot.ChatbotEvalSupport.SeedFaq;
import com.gyeongsan.cabinet.application.chatbot.ChatbotEvalSupport.SetResult;
import com.gyeongsan.cabinet.domain.chatbot.port.out.EmbeddingPort;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * FAQ 의미 검색의 품질 평가. FAQ 질문 표현(faq-seed.json)으로 색인을 만들고, 다르게 표현한 질문이 올바른 FAQ 를 찾는지, 범위 밖 질문을 잘 거르는지를
 * 측정한다.
 *
 * <p>평가 질문은 두 벌이다.
 *
 * <ul>
 *   <li>개발 세트(eval-set.json): 임계값, margin 규칙, FAQ 변형 보강을 조정할 때 보는 세트. 통과/실패 기준도 이 세트에 건다.
 *   <li>보류 세트(eval-holdout.json): FAQ 변형과도, 개발 세트와도 겹치지 않게 따로 쓴 세트. <b>보고용이다. 이 결과를 보고 FAQ 변형이나
 *       임계값을 고치면 보류 세트의 의미가 사라진다.</b>
 * </ul>
 *
 * <p>주의: 두 세트 모두 FAQ 를 쓴 사람과 같은 저자가 만들었고 실제 학생 질문이 아니다(질문 원문을 수집하지 않는 설계). 그래서 실제 분포보다 수치가 높게 나올 수
 * 있다. 어디까지나 모델과 설정을 비교하는 상대 지표로 읽어야 한다.
 *
 * <p>환경변수: CHATBOT_EVAL_MODELS_DIR(하위 디렉터리마다 model.onnx, tokenizer.json, model.properties),
 * CHATBOT_EVAL_MIN_TOP1(기본 0.80), CHATBOT_EVAL_ENFORCE(기본 true, false 면 기준 미달이어도 실패시키지 않고 리포트만
 * 남긴다). 결과는 build/chatbot-eval/report.md.
 */
class ChatbotRetrievalEvaluationTest {

    record Candidate(String id, EmbeddingPort port, boolean required) {}

    private static final String DISCLAIMER =
            """
            > **읽는 법 / 한계**
            > - 평가 질문은 FAQ 를 쓴 사람과 같은 저자가 만들었고 **실제 학생 질문이 아닙니다**(챗봇은 질문 원문을 수집하지 않는 설계). 실제 분포에서는 수치가 더 낮을 수 있습니다. 모델·설정 사이의 **상대 비교**로만 쓰세요.
            > - **개발 세트**는 임계값·margin 규칙·FAQ 변형 보강을 조정하는 데 쓰고, **보류 세트**는 FAQ 변형과 개발 세트 어느 쪽과도 겹치지 않게 따로 쓴 보고용입니다. 보류 세트 결과를 보고 FAQ 변형이나 임계값을 고치면 보류 세트의 의미가 사라집니다.
            > - FAQ 일부는 서로 답이 겹칩니다(예: 연장 5개, 대여 6개). 정답을 하나로 못 박기 어려운 질문은 `alsoAccept` 로 함께 인정했습니다(개발 세트에는 없음).
            > - 평가 질문이 87+108개 규모라 1건이 약 1% 입니다. 몇 % 차이는 오차 범위입니다.

            """;

    private static List<Candidate> candidates() throws IOException {
        List<Candidate> list = new ArrayList<>();
        list.add(new Candidate("char-ngram(기준선)", new CharNgramEmbeddingAdapter(), false));

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
                                            2),
                                    true));
                }
            }
        }
        return list;
    }

    // ---- 서식 ----

    private static String pct(double v) {
        return Double.isNaN(v) ? "—" : String.format(Locale.ROOT, "%.1f%%", v * 100);
    }

    private static String num(double v) {
        return Double.isNaN(v) ? "—" : String.format(Locale.ROOT, "%.3f", v);
    }

    private static String cell(RuleCell c) {
        return pct(c.coverage()) + " / " + pct(c.precision()) + " / " + pct(c.oosAccept());
    }

    private static String clip(String s, int max) {
        String t = s.replace("|", "\\|").replace("\n", " ");
        return t.length() > max ? t.substring(0, max) + "…" : t;
    }

    private static void appendRows(StringBuilder sb, String title, List<Row> rows) {
        sb.append(title).append("\n\n");
        sb.append(
                "| 임계값 | 답변 나감(커버리지) | 1위가 정답(답변 중) | 후보 3개 안에 정답(답변 중) | 후보에 정답 포함(전체 중) | 범위 밖에도 나감 |\n"
                        + "|---|---|---|---|---|---|\n");
        for (Row r : rows) {
            sb.append(
                    String.format(
                            Locale.ROOT,
                            "| %.2f | %s | %s | %s | %s | %s |%n",
                            r.threshold(),
                            pct(r.coverage()),
                            pct(r.top1Precision()),
                            pct(r.top3Precision()),
                            pct(r.recall()),
                            pct(r.oosAccept())));
        }
        sb.append('\n');
    }

    private static String report(
            String candidateId, boolean detailed, String setName, SetResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "#### %s — %s%n%n", candidateId, setName));
        sb.append(
                String.format(
                        Locale.ROOT,
                        "- 질문 %d개(범위 안) / %d개(범위 밖)%n"
                                + "- 1위가 정답(top-1): **%s**, 후보 3개 안에 정답(top-3): **%s**, 같은 카테고리(top-1): %s%n"
                                + "- 평균 최고 유사도: 범위 안 %s / 범위 밖 %s%n",
                        result.in().size(),
                        result.out().size(),
                        pct(result.top1()),
                        pct(result.top3()),
                        pct(result.categoryTop1()),
                        num(result.meanIn()),
                        num(result.meanOut())));

        Row rec = ChatbotEvalSupport.recommended(result);
        sb.append(
                rec == null
                        ? "- 자동 답변(MATCHED) 기준을 만족하는 임계값: **없음** (범위 밖 수락 5%↓, 1위 정답률 95%↑, 커버리지 50%↑)\n\n"
                        : String.format(
                                Locale.ROOT,
                                "- 자동 답변 기준을 만족하는 가장 낮은 임계값: **%.2f** (1위 정답률 %s, 커버리지 %s, 범위 밖 수락 %s)%n%n",
                                rec.threshold(),
                                pct(rec.top1Precision()),
                                pct(rec.coverage()),
                                pct(rec.oosAccept())));

        appendRows(
                sb,
                "임계값별 결과 — 같은 표를 \"자동 답변\"(1위가 정답인가)과 \"후보 제안\"(후보 3개 안에 정답인가) 두 관점으로 읽습니다.",
                ChatbotEvalSupport.rows(result, ChatbotEvalSupport.coarseGrid()));
        if (!detailed) {
            return sb.toString();
        }
        List<Double> fine = ChatbotEvalSupport.fineGrid(result);
        if (!fine.isEmpty()) {
            appendRows(
                    sb,
                    String.format(
                            Locale.ROOT,
                            "세밀 구간(점수가 몰린 %.2f~%.2f)",
                            fine.get(0),
                            fine.get(fine.size() - 1)),
                    ChatbotEvalSupport.rows(result, fine));
        }

        // 점수와 margin 이 "정답을 가려내는 신호"인지
        List<Probe> right = result.in().stream().filter(Probe::top1).toList();
        List<Probe> wrong = result.in().stream().filter(p -> !p.top1()).toList();
        sb.append("신호 품질(0.5 = 구분 못 함, 1.0 = 완벽)\n\n");
        sb.append("| 신호 | 정답 vs 오답 구분력(AUROC) | 정답일 때 평균 | 오답일 때 평균 |\n|---|---|---|---|\n");
        sb.append(
                String.format(
                        Locale.ROOT,
                        "| 1위 유사도 | %s | %s | %s |%n",
                        num(
                                ChatbotEvalSupport.auroc(
                                        ChatbotEvalSupport.scores(right, Probe::s1),
                                        ChatbotEvalSupport.scores(wrong, Probe::s1))),
                        num(ChatbotEvalSupport.mean(ChatbotEvalSupport.scores(right, Probe::s1))),
                        num(ChatbotEvalSupport.mean(ChatbotEvalSupport.scores(wrong, Probe::s1)))));
        sb.append(
                String.format(
                        Locale.ROOT,
                        "| 1·2위 차이(margin) | %s | %s | %s |%n",
                        num(
                                ChatbotEvalSupport.auroc(
                                        ChatbotEvalSupport.scores(right, Probe::margin),
                                        ChatbotEvalSupport.scores(wrong, Probe::margin))),
                        num(
                                ChatbotEvalSupport.mean(
                                        ChatbotEvalSupport.scores(right, Probe::margin))),
                        num(
                                ChatbotEvalSupport.mean(
                                        ChatbotEvalSupport.scores(wrong, Probe::margin)))));
        List<Double> inScores = ChatbotEvalSupport.scores(result.in(), Probe::s1);
        List<Double> outScores = result.out().stream().map(o -> o.s1()).toList();
        sb.append(
                String.format(
                        Locale.ROOT,
                        "%n- 범위 안 vs 범위 밖 구분력(1위 유사도 AUROC): %s%n%n",
                        num(ChatbotEvalSupport.auroc(inScores, outScores))));

        // 자동 답변 규칙: 점수 ≥ T 그리고 margin ≥ M
        List<Double> ts = ChatbotEvalSupport.oosQuantileThresholds(result);
        if (!ts.isEmpty()) {
            double[] ms = {0, 0.01, 0.02, 0.03, 0.05};
            sb.append(
                    "자동 답변 규칙 \"1위 유사도 ≥ T 그리고 margin ≥ M\" — 칸마다 `커버리지 / 1위 정답률 / 범위 밖 수락`. "
                            + "T 는 범위 밖 질문의 최고점 분포에서 뽑았습니다(50/75/90/100%를 걸러 내는 지점).\n\n");
            sb.append("| T \\ M |");
            for (double m : ms) {
                sb.append(String.format(Locale.ROOT, " %.2f |", m));
            }
            sb.append("\n|---|");
            sb.append("---|".repeat(ms.length)).append('\n');
            for (double t : ts) {
                sb.append(String.format(Locale.ROOT, "| %.2f |", t));
                for (double m : ms) {
                    sb.append(' ').append(cell(ChatbotEvalSupport.rule(result, t, m))).append(" |");
                }
                sb.append('\n');
            }
            sb.append('\n');
        }

        // 틀린 질문 분석
        List<Map.Entry<String, Integer>> confusions = ChatbotEvalSupport.confusions(result);
        long sameCategory =
                wrong.stream()
                        .filter(
                                p ->
                                        result.categoryByKey()
                                                .get(p.predicted())
                                                .equals(
                                                        result.categoryByKey()
                                                                .get(p.item().expected())))
                        .count();
        sb.append(
                String.format(
                        Locale.ROOT,
                        "틀린 질문 %d개 중 같은 카테고리 안에서 혼동한 것 %d개(%s). 정답이 후보 3개 안에 있던 것 %d개.%n%n",
                        wrong.size(),
                        sameCategory,
                        pct(ChatbotEvalSupport.fraction(sameCategory, wrong.size())),
                        wrong.stream().filter(Probe::top3).count()));
        if (!confusions.isEmpty()) {
            sb.append("많이 혼동된 (기대 → 예측) 상위 10\n\n| 횟수 | 기대 → 예측 |\n|---|---|\n");
            confusions.stream()
                    .limit(10)
                    .forEach(
                            e ->
                                    sb.append("| ")
                                            .append(e.getValue())
                                            .append(" | ")
                                            .append(e.getKey())
                                            .append(" |\n"));
            sb.append('\n');
        }
        if (!wrong.isEmpty()) {
            sb.append("<details><summary>틀린 질문 전체(" + wrong.size() + "개)</summary>\n\n");
            sb.append(
                    "| 질문 | 기대 | 예측 | 정답 순위 | 1위 | 2위 | margin | 같은 카테고리 |\n|---|---|---|---|---|---|---|---|\n");
            for (Probe p : wrong) {
                boolean same =
                        result.categoryByKey()
                                .get(p.predicted())
                                .equals(result.categoryByKey().get(p.item().expected()));
                sb.append(
                        String.format(
                                Locale.ROOT,
                                "| %s | %s | %s | %s | %.3f | %.3f | %.3f | %s |%n",
                                clip(p.item().question(), 50),
                                p.item().expected(),
                                p.predicted(),
                                p.rank() == 0 ? "—" : String.valueOf(p.rank()),
                                p.s1(),
                                p.s2(),
                                p.margin(),
                                same ? "예" : "아니오"));
            }
            sb.append("\n</details>\n\n");
        }
        return sb.toString();
    }

    private static void appendRuleTable(StringBuilder sb, String title, SetResult result) {
        sb.append(title).append("\n\n| T \\ M |");
        for (double m : ChatbotEvalSupport.GRID_M) {
            sb.append(String.format(Locale.ROOT, " %.2f |", m));
        }
        sb.append("\n|---|").append("---|".repeat(ChatbotEvalSupport.GRID_M.length)).append('\n');
        for (double t : ChatbotEvalSupport.GRID_T) {
            sb.append(String.format(Locale.ROOT, "| %.2f |", t));
            for (double m : ChatbotEvalSupport.GRID_M) {
                sb.append(' ').append(cell(ChatbotEvalSupport.rule(result, t, m))).append(" |");
            }
            sb.append('\n');
        }
        sb.append('\n');
    }

    /** 두 세트에서 같은 (T, M) 칸을 나란히 본다. 자동 답변 규칙은 이 표로 고른다. */
    private static String commonGrid(String candidateId, SetResult dev, SetResult holdout) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "#### %s — 공통 격자(같은 T, M)%n%n", candidateId));
        sb.append(
                "칸마다 `커버리지 / 1위 정답률 / 범위 밖 수락`. 두 세트의 범위 밖 질문 난이도가 다르다(개발은 먼 도메인, 보류는 근접 도메인 포함).\n\n");
        appendRuleTable(sb, "개발 세트", dev);
        appendRuleTable(sb, "보류 세트", holdout);
        appendRuleTable(sb, "합산(개발+보류)", ChatbotEvalSupport.pool(dev, holdout));

        List<ChatbotEvalSupport.StableCell> stable = ChatbotEvalSupport.stableCells(dev, holdout);
        if (stable.isEmpty()) {
            sb.append("두 세트 모두 범위 밖 수락 0%이면서 답변이 나가는 칸이 없습니다.\n\n");
        } else {
            sb.append("두 세트 모두 범위 밖 수락 0%인 칸(두 세트 중 낮은 정밀도 순 상위 8)\n\n");
            sb.append("| T | M | 개발 | 보류 | 낮은 쪽 정밀도 | 낮은 쪽 커버리지 |\n|---|---|---|---|---|---|\n");
            stable.stream()
                    .limit(8)
                    .forEach(
                            c ->
                                    sb.append(
                                            String.format(
                                                    Locale.ROOT,
                                                    "| %.2f | %.2f | %s | %s | %s | %s |%n",
                                                    c.t(),
                                                    c.m(),
                                                    cell(c.dev()),
                                                    cell(c.holdout()),
                                                    pct(c.minPrecision()),
                                                    pct(c.minCoverage()))));
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String learningCurve(
            List<SeedFaq> faqs, CachingEmbedder embedder, EvalSet dev, EvalSet holdout) {
        int maxVariants = faqs.stream().mapToInt(f -> f.questions().size()).max().orElse(1);
        StringBuilder sb = new StringBuilder();
        sb.append(
                "FAQ 당 변형 수에 따른 정확도(변형을 k 개만 색인에 넣고, 어느 변형을 쓸지 "
                        + maxVariants
                        + "가지 순서로 돌려 평균). 변형이 늘 때 얼마나 오르는지 보는 용도이며, "
                        + "현재 FAQ 당 "
                        + minVariants(faqs)
                        + "~"
                        + maxVariants
                        + "개라 그 이상은 외삽입니다.\n\n");
        sb.append(
                "| 변형 수 k | 개발 top-1 | 개발 top-3 | 보류 top-1 | 보류 top-3 |\n|---|---|---|---|---|\n");
        for (int k = 1; k <= maxVariants; k++) {
            double[] sum = new double[4];
            for (int offset = 0; offset < maxVariants; offset++) {
                SetResult d =
                        ChatbotEvalSupport.probe(
                                faqs, ChatbotEvalSupport.firstK(faqs, k, offset), embedder, dev);
                SetResult h =
                        ChatbotEvalSupport.probe(
                                faqs,
                                ChatbotEvalSupport.firstK(faqs, k, offset),
                                embedder,
                                holdout);
                sum[0] += d.top1();
                sum[1] += d.top3();
                sum[2] += h.top1();
                sum[3] += h.top3();
            }
            sb.append(
                    String.format(
                            Locale.ROOT,
                            "| %d | %s | %s | %s | %s |%n",
                            k,
                            pct(sum[0] / maxVariants),
                            pct(sum[1] / maxVariants),
                            pct(sum[2] / maxVariants),
                            pct(sum[3] / maxVariants)));
        }
        sb.append('\n');
        return sb.toString();
    }

    private static int minVariants(List<SeedFaq> faqs) {
        return faqs.stream().mapToInt(f -> f.questions().size()).min().orElse(0);
    }

    // ---- 평가 ----

    @Test
    @DisplayName("FAQ 의미 검색 평가: 재표현 질문을 올바른 FAQ 로 찾고, 범위 밖 질문은 거르는지 측정한다")
    void evaluate() throws Exception {
        List<SeedFaq> faqs = ChatbotEvalSupport.loadFaqs();
        EvalSet dev = ChatbotEvalSupport.loadEvalSet("/chatbot/eval-set.json");
        EvalSet holdout = ChatbotEvalSupport.loadEvalSet("/chatbot/eval-holdout.json");
        assertThat(faqs).hasSizeGreaterThanOrEqualTo(20);
        assertThat(dev.inScope()).hasSizeGreaterThanOrEqualTo(60);
        assertThat(dev.outOfScope()).hasSizeGreaterThanOrEqualTo(15);
        assertThat(holdout.inScope()).hasSizeGreaterThanOrEqualTo(100);
        assertThat(holdout.outOfScope()).hasSizeGreaterThanOrEqualTo(25);

        // 평가 질문이 가리키는 FAQ 가 실제로 있어야 한다.
        List<String> keys = faqs.stream().map(SeedFaq::seedKey).toList();
        for (EvalSet set : List.of(dev, holdout)) {
            assertThat(set.inScope()).allSatisfy(i -> assertThat(keys).containsAll(i.accepted()));
        }
        // 평가 질문이 색인된 FAQ 질문 표현과 글자 그대로 같으면 평가가 아니라 암기다.
        // 보류 세트는 개발 세트와도 겹치면 안 된다(개발 세트를 보고 변형을 쓰게 되면 보류 세트가 오염된다).
        Set<String> indexed = new LinkedHashSet<>();
        faqs.forEach(f -> indexed.addAll(f.questions()));
        Set<String> devQuestions = new LinkedHashSet<>();
        dev.inScope().forEach(i -> devQuestions.add(i.question()));
        devQuestions.addAll(dev.outOfScope());
        for (EvalSet set : List.of(dev, holdout)) {
            assertThat(set.inScope().stream().map(EvalItem::question).toList())
                    .doesNotContainAnyElementsOf(indexed);
            assertThat(set.outOfScope()).doesNotContainAnyElementsOf(indexed);
        }
        assertThat(holdout.inScope().stream().map(EvalItem::question).toList())
                .doesNotContainAnyElementsOf(devQuestions);
        assertThat(holdout.outOfScope()).doesNotContainAnyElementsOf(devQuestions);

        double minTop1 =
                Double.parseDouble(System.getenv().getOrDefault("CHATBOT_EVAL_MIN_TOP1", "0.80"));
        boolean enforce =
                Boolean.parseBoolean(System.getenv().getOrDefault("CHATBOT_EVAL_ENFORCE", "true"));
        List<Candidate> candidates = candidates();

        StringBuilder report = new StringBuilder("## 챗봇 FAQ 의미 검색 평가\n\n");
        report.append(
                String.format(
                        Locale.ROOT,
                        "FAQ %d건(질문 표현 %d개). 개발 세트 %d+%d, 보류 세트 %d+%d (범위 안+범위 밖). 기준 적용: %s%n%n",
                        faqs.size(),
                        indexed.size(),
                        dev.inScope().size(),
                        dev.outOfScope().size(),
                        holdout.inScope().size(),
                        holdout.outOfScope().size(),
                        enforce ? "켜짐(개발 세트 기준)" : "꺼짐(리포트만)"));
        report.append(DISCLAIMER);

        List<String> failures = new ArrayList<>();
        try {
            for (Candidate candidate : candidates) {
                CachingEmbedder embedder = new CachingEmbedder(candidate.port());
                List<List<Integer>> all = ChatbotEvalSupport.allVariants(faqs);
                SetResult devResult = ChatbotEvalSupport.probe(faqs, all, embedder, dev);
                SetResult holdoutResult = ChatbotEvalSupport.probe(faqs, all, embedder, holdout);

                report.append(String.format(Locale.ROOT, "### %s%n%n", candidate.id()));
                report.append(report(candidate.id(), candidate.required(), "개발 세트", devResult));
                report.append(
                        report(candidate.id(), candidate.required(), "보류 세트(보고용)", holdoutResult));
                if (candidate.required()) {
                    report.append(
                            String.format(Locale.ROOT, "#### %s — 변형 수 학습곡선%n%n", candidate.id()));
                    report.append(learningCurve(faqs, embedder, dev, holdout));

                    if (devResult.top1() < minTop1) {
                        failures.add(
                                String.format(
                                        Locale.ROOT,
                                        "%s: 개발 세트 top-1 %.1f%% < 기준 %.0f%%",
                                        candidate.id(),
                                        devResult.top1() * 100,
                                        minTop1 * 100));
                    }
                    if (ChatbotEvalSupport.recommended(devResult) == null) {
                        failures.add(candidate.id() + ": 개발 세트에서 자동 답변 기준을 만족하는 임계값이 없음");
                    }
                }
            }
            if (candidates.size() == 1) {
                report.append("> 실제 모델 후보가 없어 글자 겹침 기준선만 측정했습니다(CHATBOT_EVAL_MODELS_DIR 미설정).\n");
            }
        } finally {
            for (Candidate candidate : candidates) {
                if (candidate.port() instanceof AutoCloseable closeable) {
                    closeable.close();
                }
            }
        }

        Path out = Path.of("build/chatbot-eval/report.md");
        Files.createDirectories(out.getParent());
        Files.writeString(out, report.toString(), StandardCharsets.UTF_8);
        System.out.println(report);

        if (enforce) {
            assertThat(failures).as("평가 기준 미달").isEmpty();
        }
    }

    /**
     * FAQ 변형이 평가 질문을 거의 베낀 것이 아닌지 확인한다. 글자가 완전히 같은지는 evaluate() 가 막고, 여기서는 같은 FAQ 를 가리키는 평가 질문과 글자
     * 겹침이 매우 큰(어순·조사만 바꾼) 변형을 막는다. 다른 FAQ 의 질문끼리 틀(예: "~는 어디서 하나요")을 공유하는 것은 정상이라 보지 않는다.
     */
    @Test
    @DisplayName("FAQ 변형이 같은 FAQ 를 가리키는 평가 질문의 거의 복사본이면 실패한다")
    void variantsAreNotNearCopiesOfEvalQuestions() throws Exception {
        double limit = 0.85;
        List<SeedFaq> faqs = ChatbotEvalSupport.loadFaqs();
        CharNgramEmbeddingAdapter ngram = new CharNgramEmbeddingAdapter();
        List<String> violations = new ArrayList<>();
        for (String resource : List.of("/chatbot/eval-set.json", "/chatbot/eval-holdout.json")) {
            EvalSet set = ChatbotEvalSupport.loadEvalSet(resource);
            for (EvalItem item : set.inScope()) {
                float[] q = ngram.embed(TextNormalizer.normalize(item.question()));
                for (SeedFaq faq : faqs) {
                    if (!item.accepted().contains(faq.seedKey())) {
                        continue;
                    }
                    for (String variant : faq.questions()) {
                        float[] v = ngram.embed(TextNormalizer.normalize(variant));
                        double dot = 0;
                        for (int i = 0; i < q.length; i++) {
                            dot += (double) q[i] * v[i];
                        }
                        if (dot >= limit) {
                            violations.add(
                                    String.format(
                                            Locale.ROOT,
                                            "%.2f | FAQ 변형 \"%s\" ≈ 평가 질문 \"%s\" (%s)",
                                            dot,
                                            variant,
                                            item.question(),
                                            resource));
                        }
                    }
                }
            }
        }
        assertThat(violations).as("평가 질문을 거의 베낀 FAQ 변형").isEmpty();
    }
}
