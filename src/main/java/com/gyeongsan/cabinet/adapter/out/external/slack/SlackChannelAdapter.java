package com.gyeongsan.cabinet.adapter.out.external.slack;

import com.fasterxml.jackson.databind.JsonNode;
import com.gyeongsan.cabinet.domain.alarm.model.SlackChannelException;
import com.gyeongsan.cabinet.domain.alarm.model.SlackChannelMessage;
import com.gyeongsan.cabinet.domain.alarm.model.SlackHistory;
import com.gyeongsan.cabinet.domain.alarm.port.out.SlackChannelPort;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * 슬랙 Web API 로 채널의 최상위 메시지를 읽는다(conversations.history). 필요한 권한: 공개 채널 channels:history, 비공개 채널
 * groups:history 이며 봇이 채널에 초대되어 있어야 한다. 스레드 답글은 읽지 않는다.
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class SlackChannelAdapter implements SlackChannelPort {

    static final int PAGE_SIZE = 100;
    static final int MAX_PAGES = 5;

    private final WebClient webClient;

    @Value("${slack.token}")
    private String botToken;

    @Value("${app.slack-report.api-base-url:https://slack.com/api}")
    private String apiBaseUrl;

    @Override
    public SlackHistory fetchNewerThan(String channelId, String oldestTs) {
        List<SlackChannelMessage> collected = new ArrayList<>();
        String pageCursor = null;
        boolean truncated = false;

        for (int page = 1; ; page++) {
            JsonNode body = history(channelId, oldestTs, PAGE_SIZE, pageCursor);
            for (JsonNode message : body.path("messages")) {
                collected.add(toMessage(message));
            }

            boolean hasMore = body.path("has_more").asBoolean(false);
            String next = body.path("response_metadata").path("next_cursor").asText("");
            if (!hasMore || next.isEmpty()) {
                break;
            }
            if (page >= MAX_PAGES) {
                // 최신 쪽부터 가져오므로, 한도를 넘으면 가장 오래된 일부를 못 가져온다.
                truncated = true;
                break;
            }
            pageCursor = next;
        }

        // 응답은 최신순이다. 오래된 것부터 처리하도록 뒤집는다.
        Collections.reverse(collected);
        return new SlackHistory(List.copyOf(collected), truncated);
    }

    @Override
    public Optional<String> latestTs(String channelId) {
        JsonNode body = history(channelId, null, 1, null);
        JsonNode messages = body.path("messages");
        if (!messages.isArray() || messages.isEmpty()) {
            return Optional.empty();
        }
        String ts = messages.get(0).path("ts").asText("");
        return ts.isEmpty() ? Optional.empty() : Optional.of(ts);
    }

    @Override
    public Optional<String> permalink(String channelId, String ts) {
        try {
            JsonNode body =
                    call(
                            "chat.getPermalink",
                            b -> b.queryParam("channel", channelId).queryParam("message_ts", ts));
            String link = body.path("permalink").asText("");
            return link.isEmpty() ? Optional.empty() : Optional.of(link);
        } catch (SlackChannelException e) {
            log.debug("[SlackReport] 원문 링크를 얻지 못했습니다: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private JsonNode history(String channelId, String oldestTs, int limit, String pageCursor) {
        return call(
                "conversations.history",
                b -> {
                    b.queryParam("channel", channelId).queryParam("limit", limit);
                    if (oldestTs != null) {
                        // inclusive 기본값 false: 커서 자신은 제외한다.
                        b.queryParam("oldest", oldestTs);
                    }
                    if (pageCursor != null) {
                        b.queryParam("cursor", pageCursor);
                    }
                    return b;
                });
    }

    private JsonNode call(
            String method,
            java.util.function.Function<
                            org.springframework.web.util.UriBuilder,
                            org.springframework.web.util.UriBuilder>
                    params) {
        JsonNode body;
        try {
            body =
                    webClient
                            .get()
                            .uri(
                                    apiBaseUrl + "/" + method,
                                    uriBuilder -> params.apply(uriBuilder).build())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + botToken)
                            .retrieve()
                            .bodyToMono(JsonNode.class)
                            .block();
        } catch (WebClientResponseException.TooManyRequests e) {
            String retryAfter = e.getHeaders().getFirst(HttpHeaders.RETRY_AFTER);
            throw new SlackChannelException(
                    method + " 요청 한도 초과(429), Retry-After=" + retryAfter, e);
        } catch (RuntimeException e) {
            throw new SlackChannelException(method + " 호출 실패: " + e.getMessage(), e);
        }

        if (body == null || !body.path("ok").asBoolean(false)) {
            // 오류 코드만 기록한다(토큰 등 민감한 값은 응답에 없다).
            String error = body == null ? "빈 응답" : body.path("error").asText("unknown");
            throw new SlackChannelException(method + " 실패: " + error + hint(error));
        }
        return body;
    }

    private static String hint(String error) {
        return switch (error) {
            case "not_in_channel" -> " (봇을 채널에 초대해야 합니다)";
            case "missing_scope" -> " (앱에 channels:history 또는 groups:history 권한이 필요합니다)";
            case "channel_not_found" -> " (채널 ID 를 확인하세요)";
            case "invalid_auth", "not_authed", "token_revoked" -> " (봇 토큰을 확인하세요)";
            default -> "";
        };
    }

    private static SlackChannelMessage toMessage(JsonNode message) {
        String user = message.path("user").asText("");
        if (user.isEmpty()) {
            user = message.path("bot_id").asText("");
        }
        String subtype = message.path("subtype").asText("");
        return new SlackChannelMessage(
                message.path("ts").asText(""),
                user.isEmpty() ? null : user,
                message.path("text").asText(""),
                subtype.isEmpty() ? null : subtype,
                message.path("reply_count").asInt(0),
                message.path("files").isArray() ? message.path("files").size() : 0);
    }
}
