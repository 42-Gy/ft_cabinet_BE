package com.gyeongsan.cabinet.utils;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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

        HttpHeaders headers = new HttpHeaders();
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            JsonNode response = webClient.post()
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
    public int getYesterdayLogtimeMinutes(String intraId) {
        synchronized (this) {
            if (this.accessToken == null) {
                generateToken();
            }
        }

        ZoneId kstZone = ZoneId.of("Asia/Seoul");
        ZonedDateTime nowKst = ZonedDateTime.now(kstZone);
        ZonedDateTime yesterdayStartKst = nowKst.minusDays(1).toLocalDate().atStartOfDay(kstZone);
        ZonedDateTime yesterdayEndKst = yesterdayStartKst.plusDays(1).minusSeconds(1);

        ZonedDateTime reqStart = yesterdayStartKst.withZoneSameInstant(ZoneId.of("UTC"));
        ZonedDateTime reqEnd = yesterdayEndKst.withZoneSameInstant(ZoneId.of("UTC"));

        String rangeStart = reqStart.format(DateTimeFormatter.ISO_INSTANT);
        String rangeEnd = reqEnd.format(DateTimeFormatter.ISO_INSTANT);

        String url = String.format(
                "%s/v2/users/%s/locations?range[begin_at]=%s,%s&page[size]=100",
                ftApiRootUrl, intraId, rangeStart, rangeEnd);

        return callApiWithRetry(url, intraId, reqStart, reqEnd);
    }

    @RateLimiter(name = "ftApi")
    public int getLogtimeBetween(String intraId, LocalDateTime start, LocalDateTime end) {
        synchronized (this) {
            if (this.accessToken == null) {
                generateToken();
            }
        }

        ZoneId zone = ZoneId.of("Asia/Seoul");

        ZonedDateTime zonedStart = start.atZone(zone);
        ZonedDateTime zonedEnd = end.atZone(zone);

        String rangeStart = zonedStart
                .withZoneSameInstant(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ISO_INSTANT);

        String rangeEnd = zonedEnd
                .withZoneSameInstant(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ISO_INSTANT);

        String url = String.format(
                "%s/v2/users/%s/locations?range[begin_at]=%s,%s&page[size]=100",
                ftApiRootUrl, intraId, rangeStart, rangeEnd);

        return callApiWithRetry(url, intraId, zonedStart, zonedEnd);
    }

    private int callApiWithRetry(String url, String intraId, ZonedDateTime start, ZonedDateTime end) {
        try {
            return requestLogtime(url, start, end);
        } catch (WebClientResponseException.Unauthorized e) {
            log.warn("🔄 [401 Unauthorized] 토큰 만료. 재발급 후 재시도... ({})", intraId);
            try {
                generateToken();
                return requestLogtime(url, start, end);
            } catch (Exception ex) {
                log.error("❌ 재시도 실패 ({}): {}", intraId, ex.getMessage());
                throw new RuntimeException("42 API call failed after retry", ex);
            }
        } catch (Exception e) {
            log.error("❌ API 호출 실패 ({}): {}", intraId, e.getMessage());
            throw new RuntimeException("42 API call failed", e);
        }
    }

    private int requestLogtime(String url, ZonedDateTime reqStart, ZonedDateTime reqEnd) {
        JsonNode locations = webClient.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.accessToken)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        long totalSeconds = 0;

        if (locations != null) {
            for (JsonNode loc : locations) {
                String beginStr = loc.get("begin_at").asText();
                JsonNode endNode = loc.get("end_at");

                String endStr = (endNode == null || endNode.isNull()) ? null : endNode.asText();

                if (endStr == null)
                    continue;

                ZonedDateTime begin = ZonedDateTime.parse(beginStr);
                ZonedDateTime end = ZonedDateTime.parse(endStr);

                // 범위 교차 검사
                if (end.isBefore(reqStart) || begin.isAfter(reqEnd)) {
                    continue;
                }

                // 범위 자르기 (Clamping)
                ZonedDateTime effectiveBegin = begin.isBefore(reqStart) ? reqStart : begin;
                ZonedDateTime effectiveEnd = end.isAfter(reqEnd) ? reqEnd : end;

                long seconds = Duration.between(effectiveBegin, effectiveEnd).getSeconds();
                if (seconds > 0) {
                    totalSeconds += seconds;
                }
            }
        }

        return (int) (totalSeconds / 60);
    }
}