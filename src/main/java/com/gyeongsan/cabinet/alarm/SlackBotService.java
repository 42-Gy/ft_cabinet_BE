package com.gyeongsan.cabinet.alarm;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Log4j2
public class SlackBotService {

    @Value("${slack.token}")
    private String botToken;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendDm(String intraId, String messageContent) {
        try {
            String slackUserId = findSlackIdByIntraId(intraId);

            if (slackUserId == null) {
                log.error("❌ 해당 Intra ID({})를 가진 슬랙 유저를 찾을 수 없습니다.", intraId);
                return;
            }

            sendMessage(slackUserId, messageContent);

        } catch (Exception e) {
            log.error("❌ 슬랙 DM 전송 중 오류 발생: {}", e.getMessage());
        }
    }

    private String findSlackIdByIntraId(String intraId) {
        String url = "https://slack.com/api/users.list";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(botToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            JsonNode body = response.getBody();

            if (body != null && body.get("ok").asBoolean()) {
                JsonNode members = body.get("members");
                if (members.isArray()) {
                    for (JsonNode member : members) {
                        if (member.has("name") && member.get("name").asText().equals(intraId)) {
                            return member.get("id").asText();
                        }
                    }
                }
            } else {
                log.warn("슬랙 유저 리스트 조회 실패 (응답): {}", body);
            }
        } catch (Exception e) {
            log.error("슬랙 API 호출 실패: {}", e.getMessage());
        }
        return null;
    }

    private void sendMessage(String channelId, String text) {
        String url = "https://slack.com/api/chat.postMessage";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(botToken);

        Map<String, Object> body = new HashMap<>();
        body.put("channel", channelId);
        body.put("text", text);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(url, request, String.class);

        log.info("✅ DM 전송 성공! Target: {}", channelId);
    }
}
