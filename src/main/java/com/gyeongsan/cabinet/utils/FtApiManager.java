package com.gyeongsan.cabinet.utils;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

@Component
@RequiredArgsConstructor
@Log4j2
public class FtApiManager {

    @Value("${spring.security.oauth2.client.registration.42.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.42.client-secret}")
    private String clientSecret;

    @Value("${app.ft-api.root-url}")
    private String ftApiRootUrl;

    @Value("${app.ft-api.token-url}")
    private String ftApiTokenUrl;

    private final WebClient webClient;

    private volatile String accessToken;

    private synchronized void generateToken() {
        String url = ftApiTokenUrl;

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        try {
            JsonNode response =
                    webClient
                            .post()
                            .uri(url)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .body(BodyInserters.fromValue(body))
                            .retrieve()
                            .bodyToMono(JsonNode.class)
                            .block();

            if (response != null) {
                this.accessToken = response.get("access_token").asText();
                log.info("✅ 42 API 토큰 발급/갱신 성공");
            }
        } catch (Exception e) {
            log.error("❌ 토큰 발급 실패: {}", e.getMessage());
            throw new RuntimeException("42 API 토큰 발급 실패", e);
        }
    }

    @RateLimiter(name = "ftApi")
    public int getLogtimeBetween(String intraId, LocalDateTime start, LocalDateTime end) {
        synchronized (this) {
            if (this.accessToken == null) {
                generateToken();
            }
        }

        LocalDate startDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();

        String url = String.format("%s/v2/users/%s/locations_stats", ftApiRootUrl, intraId);

        return callApiWithRetry(url, intraId, startDate, endDate);
    }

    private int callApiWithRetry(
            String url, String intraId, LocalDate startDate, LocalDate endDate) {
        try {
            return requestLocationStats(url, startDate, endDate);
        } catch (WebClientResponseException.Unauthorized e) {
            log.warn("🔄 [401 Unauthorized] 토큰 만료. 재발급 후 재시도... ({})", intraId);
            try {
                generateToken();
                return requestLocationStats(url, startDate, endDate);
            } catch (Exception ex) {
                log.error("❌ 재시도 실패 ({}): {}", intraId, ex.getMessage());
                return -1;
            }
        } catch (Exception e) {
            log.error("❌ API 호출 실패 ({}): {}", intraId, e.getMessage());
            return -1;
        }
    }

    private int requestLocationStats(String url, LocalDate startDate, LocalDate endDate) {
        JsonNode stats =
                webClient
                        .get()
                        .uri(url)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .retryWhen(
                                Retry.backoff(3, Duration.ofSeconds(2))
                                        .maxBackoff(Duration.ofSeconds(10))
                                        .filter(FtApiManager::isRetryable)
                                        .doBeforeRetry(
                                                s ->
                                                        log.warn(
                                                                "🔁 42 API 재시도 {}회차: {}",
                                                                s.totalRetries() + 1,
                                                                s.failure().getMessage()))
                                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                        .block();

        return sumLogtimeMinutes(stats, startDate, endDate);
    }

    static int sumLogtimeMinutes(JsonNode stats, LocalDate startDate, LocalDate endDate) {
        if (stats == null || stats.isEmpty()) {
            return 0;
        }

        long totalSeconds = 0;

        for (Map.Entry<String, JsonNode> entry : stats.properties()) {
            String dateStr = entry.getKey();
            String timeStr = entry.getValue().asText();

            LocalDate date = LocalDate.parse(dateStr);

            if ((date.isEqual(startDate) || date.isAfter(startDate))
                    && (date.isEqual(endDate) || date.isBefore(endDate))) {

                String[] parts = timeStr.split(":");
                if (parts.length == 3) {
                    int hours = Integer.parseInt(parts[0]);
                    int minutes = Integer.parseInt(parts[1]);
                    double seconds = Double.parseDouble(parts[2]);
                    totalSeconds += hours * 3600L + minutes * 60L + (long) seconds;
                }
            }
        }

        return (int) (totalSeconds / 60);
    }

    static boolean isRetryable(Throwable e) {
        if (e instanceof WebClientResponseException ex) {
            return ex.getStatusCode().value() == 429 || ex.getStatusCode().is5xxServerError();
        }
        return e instanceof WebClientRequestException;
    }
}
