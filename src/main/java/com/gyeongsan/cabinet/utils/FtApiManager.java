package com.gyeongsan.cabinet.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Log4j2
public class FtApiManager {

    @Value("${spring.security.oauth2.client.registration.42.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.42.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private String accessToken;

    private void generateToken() {
        String url = "https://api.intra.42.fr/oauth/token";
        HttpHeaders headers = new HttpHeaders();
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, request, JsonNode.class);
            this.accessToken = response.getBody().get("access_token").asText();
            log.info("42 API 토큰 발급 성공");
        } catch (Exception e) {
            log.error("토큰 발급 실패: {}", e.getMessage());
        }
    }

    public int getYesterdayLogtimeMinutes(String intraId) {
        if (this.accessToken == null) generateToken();

        ZoneId kstZone = ZoneId.of("Asia/Seoul");
        ZonedDateTime nowKst = ZonedDateTime.now(kstZone);
        ZonedDateTime yesterdayStartKst = nowKst.minusDays(1).toLocalDate().atStartOfDay(kstZone);
        ZonedDateTime yesterdayEndKst = yesterdayStartKst.plusDays(1).minusSeconds(1);

        String rangeStart = yesterdayStartKst.withZoneSameInstant(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT);
        String rangeEnd = yesterdayEndKst.withZoneSameInstant(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT);

        String url = String.format(
                "https://api.intra.42.fr/v2/users/%s/locations?range[begin_at]=%s,%s&page[size]=100",
                intraId, rangeStart, rangeEnd
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + this.accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            JsonNode locations = response.getBody();

            long totalSeconds = 0;
            for (JsonNode loc : locations) {
                String beginStr = loc.get("begin_at").asText();
                String endStr = loc.get("end_at").isNull() ? null : loc.get("end_at").asText();

                if (endStr == null) continue;

                ZonedDateTime begin = ZonedDateTime.parse(beginStr);
                ZonedDateTime end = ZonedDateTime.parse(endStr);

                totalSeconds += Duration.between(begin, end).getSeconds();
            }

            return (int) (totalSeconds / 60);

        } catch (Exception e) {
            log.error("API 호출 실패 ({}): {}", intraId, e.getMessage());
            return 0;
        }
    }
}
