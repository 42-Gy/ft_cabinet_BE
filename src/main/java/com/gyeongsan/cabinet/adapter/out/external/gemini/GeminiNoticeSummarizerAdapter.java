package com.gyeongsan.cabinet.adapter.out.external.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyeongsan.cabinet.domain.kakaonotify.model.NoticeSummaryException;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.NoticeSummarizerPort;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.netty.channel.ChannelOption;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * Gemini API({@code generateContent})로 공지를 요약한다.
 *
 * <ul>
 *   <li>API 키는 요청 헤더({@code x-goog-api-key})로만 보낸다. URL·로그·예외 메시지·{@code toString} 에 남기지 않는다.
 *   <li>연결/응답 시간 제한을 둔다. 인프라 오류(시간 초과, 5xx, 연결 실패)는 서킷 브레이커에 실패로 기록해 반복 장애 때 호출 자체를 건너뛴다. 빈 응답이나 안전
 *       차단은 "요약 없음"일 뿐 장애가 아니므로 실패로 세지 않는다.
 *   <li>오류 응답 본문과 공지 본문은 로그·예외에 담지 않는다.
 * </ul>
 */
@Log4j2
public class GeminiNoticeSummarizerAdapter implements NoticeSummarizerPort {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern MODEL_NAME = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    /** 생각(thinking) 토큰도 출력 한도에 포함되므로 요약문 길이에 비해 넉넉히 둔다. */
    private static final int MAX_OUTPUT_TOKENS = 1024;

    private final WebClient webClient;
    private final String model;
    private final String apiKey;
    private final String thinkingLevel;
    private final Duration timeout;
    private final CircuitBreaker circuitBreaker;

    public GeminiNoticeSummarizerAdapter(
            WebClient webClient,
            String model,
            String apiKey,
            String thinkingLevel,
            Duration timeout,
            CircuitBreaker circuitBreaker) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Gemini API 키가 비어 있습니다.");
        }
        if (model == null || !MODEL_NAME.matcher(model).matches()) {
            // 모델 이름은 URL 경로에 들어가므로 형식을 제한한다.
            throw new IllegalArgumentException("Gemini 모델 이름 형식이 올바르지 않습니다.");
        }
        this.webClient = webClient;
        this.model = model;
        this.apiKey = apiKey.strip();
        this.thinkingLevel = thinkingLevel == null ? "" : thinkingLevel.strip();
        this.timeout = timeout;
        this.circuitBreaker = circuitBreaker;
    }

    /** 연결/응답 시간 제한이 있는 WebClient. 기준 주소는 호출하는 쪽(설정)이 정한다. */
    public static WebClient createWebClient(String baseUrl, Duration timeout) {
        HttpClient httpClient =
                HttpClient.create()
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) timeout.toMillis())
                        .responseTimeout(timeout);
        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public Optional<String> summarize(String text, int maxChars) {
        try {
            return circuitBreaker.executeSupplier(() -> call(text, maxChars));
        } catch (CallNotPermittedException e) {
            throw new NoticeSummaryException("요약 서킷 브레이커가 열려 있어 호출하지 않았습니다.");
        }
    }

    /** 키를 노출하는 값이 실수로 출력되지 않게 한다. */
    @Override
    public String toString() {
        return "GeminiNoticeSummarizerAdapter[model=" + model + "]";
    }

    private Optional<String> call(String text, int maxChars) {
        Map<String, Object> generationConfig = new java.util.LinkedHashMap<>();
        generationConfig.put("maxOutputTokens", MAX_OUTPUT_TOKENS);
        if (!thinkingLevel.isEmpty()) {
            generationConfig.put("thinkingConfig", Map.of("thinkingLevel", thinkingLevel));
        }
        Map<String, Object> body =
                Map.of(
                        "systemInstruction",
                        Map.of(
                                "parts",
                                List.of(
                                        Map.of(
                                                "text",
                                                GeminiNoticePrompt.systemInstruction(maxChars)))),
                        "contents",
                        List.of(
                                Map.of(
                                        "role",
                                        "user",
                                        "parts",
                                        List.of(
                                                Map.of(
                                                        "text",
                                                        GeminiNoticePrompt.userContent(text))))),
                        "generationConfig",
                        generationConfig);

        String response;
        try {
            response =
                    webClient
                            .post()
                            .uri("/v1beta/models/{model}:generateContent", model)
                            .header("x-goog-api-key", apiKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(body)
                            .exchangeToMono(
                                    r -> {
                                        if (!r.statusCode().is2xxSuccessful()) {
                                            // 오류 본문은 읽지도 남기지도 않는다.
                                            return r.releaseBody()
                                                    .then(
                                                            reactor.core.publisher.Mono.error(
                                                                    new NoticeSummaryException(
                                                                            "Gemini 응답 상태 "
                                                                                    + r.statusCode()
                                                                                            .value())));
                                        }
                                        return r.bodyToMono(String.class);
                                    })
                            .block(timeout.plusSeconds(2));
        } catch (NoticeSummaryException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new NoticeSummaryException("Gemini 호출에 실패했습니다: " + e.getClass().getSimpleName());
        }
        return parse(response);
    }

    /** 요약이 없으면(차단, 후보 없음, 출력 한도 도달, 빈 문자열) 빈 값. 해석할 수 없는 JSON 은 장애로 본다. */
    private Optional<String> parse(String response) {
        if (response == null || response.isBlank()) {
            return Optional.empty();
        }
        JsonNode root;
        try {
            root = MAPPER.readTree(response);
        } catch (Exception e) {
            throw new NoticeSummaryException("Gemini 응답을 해석하지 못했습니다.");
        }
        if (root.path("promptFeedback").hasNonNull("blockReason")) {
            log.warn("[NoticeSummary] 입력이 차단되어 요약이 없습니다.");
            return Optional.empty();
        }
        JsonNode candidate = root.path("candidates").path(0);
        String finish = candidate.path("finishReason").asText("");
        if (!finish.isEmpty() && !"STOP".equals(finish)) {
            // MAX_TOKENS 등: 중간에 끊긴 요약은 믿지 않는다.
            log.warn("[NoticeSummary] 정상 종료가 아니라 요약을 쓰지 않습니다 - finishReason: {}", finish);
            return Optional.empty();
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode part : candidate.path("content").path("parts")) {
            if (part.path("thought").asBoolean(false)) {
                continue;
            }
            sb.append(part.path("text").asText(""));
        }
        String result = sb.toString().strip();
        if (result.isEmpty()) {
            log.warn("[NoticeSummary] 응답에 요약문이 없습니다.");
            return Optional.empty();
        }
        return Optional.of(result);
    }
}
