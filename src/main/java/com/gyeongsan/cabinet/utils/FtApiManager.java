package com.gyeongsan.cabinet.utils;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

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

    private final RestTemplate restTemplate;

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
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, request, JsonNode.class);

            if (response.getBody() != null) {
                this.accessToken = response.getBody().get("access_token").asText();
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

        String rangeStart = yesterdayStartKst.withZoneSameInstant(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ISO_INSTANT);
        String rangeEnd = yesterdayEndKst.withZoneSameInstant(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ISO_INSTANT);

        String url = String.format(
                "%s/v2/users/%s/locations?range[begin_at]=%s,%s&page[size]=100",
                ftApiRootUrl, intraId, rangeStart, rangeEnd);

        return callApiWithRetry(url, intraId);
    }

    @RateLimiter(name = "ftApi")
    public int getLogtimeBetween(String intraId, LocalDateTime start, LocalDateTime end) {
        synchronized (this) {
            if (this.accessToken == null) {
                generateToken();
            }
        }

        ZoneId zone = ZoneId.of("Asia/Seoul");

        String rangeStart = start.atZone(zone)
                .withZoneSameInstant(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ISO_INSTANT);

        String rangeEnd = end.atZone(zone)
                .withZoneSameInstant(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ISO_INSTANT);

        String url = String.format(
                "%s/v2/users/%s/locations?range[begin_at]=%s,%s&page[size]=100",
                ftApiRootUrl, intraId, rangeStart, rangeEnd);

        return callApiWithRetry(url, intraId);
    }

    private int callApiWithRetry(String url, String intraId) {
        try {
            return requestLogtime(url);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("🔄 [401 Unauthorized] 토큰 만료. 재발급 후 재시도... ({})", intraId);
            try {
                generateToken();
                return requestLogtime(url);
            } catch (Exception ex) {
                log.error("❌ 재시도 실패 ({}): {}", intraId, ex.getMessage());
                return 0;
            }
        } catch (Exception e) {
            log.error("❌ API 호출 실패 ({}): {}", intraId, e.getMessage());
            return 0;
        }
    }

    private int requestLogtime(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + this.accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

        JsonNode locations = response.getBody();
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

                totalSeconds += Duration.between(begin, end).getSeconds();
            }
        }

        return (int) (totalSeconds / 60);
    }
}