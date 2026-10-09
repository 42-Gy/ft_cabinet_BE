package com.gyeongsan.cabinet.adapter.out.external.slack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gyeongsan.cabinet.domain.alarm.model.SlackChannelException;
import com.gyeongsan.cabinet.domain.alarm.model.SlackChannelMessage;
import com.gyeongsan.cabinet.domain.alarm.model.SlackHistory;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/** 실제 네트워크 없이 슬랙 응답(합성 데이터)을 흉내 내 요청 모양과 해석을 확인한다. */
class SlackChannelAdapterTest {

    private final List<ClientRequest> requests = new ArrayList<>();

    private static ClientResponse json(HttpStatus status, String body) {
        return ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build();
    }

    private SlackChannelAdapter adapter(Function<ClientRequest, ClientResponse> responder) {
        WebClient webClient =
                WebClient.builder()
                        .exchangeFunction(
                                request -> {
                                    requests.add(request);
                                    return Mono.just(responder.apply(request));
                                })
                        .build();
        SlackChannelAdapter adapter = new SlackChannelAdapter(webClient);
        ReflectionTestUtils.setField(adapter, "botToken", "xoxb-test-token");
        ReflectionTestUtils.setField(adapter, "apiBaseUrl", "https://slack.example.test/api");
        return adapter;
    }

    private static String query(ClientRequest request, String name) {
        return org.springframework.web.util.UriComponentsBuilder.fromUri(request.url())
                .build()
                .getQueryParams()
                .getFirst(name);
    }

    @Test
    @DisplayName("커서 이후 메시지를 요청하고(커서 자신 제외), 최신순 응답을 오래된 순으로 돌려준다")
    void fetchesNewerThanAndReversesToOldestFirst() {
        SlackChannelAdapter adapter =
                adapter(
                        r ->
                                json(
                                        HttpStatus.OK,
                                        """
                                        {"ok": true, "has_more": false, "messages": [
                                          {"ts": "1700000300.000100", "user": "U2", "text": "셋째", "reply_count": 2},
                                          {"ts": "1700000200.000100", "user": "U1", "text": "둘째",
                                           "files": [{"id": "F1"}, {"id": "F2"}]},
                                          {"ts": "1700000100.000100", "subtype": "channel_join", "user": "U1", "text": "joined"}
                                        ]}
                                        """));

        SlackHistory history = adapter.fetchNewerThan("C0REPORT", "1700000000.000000");

        assertThat(history.truncated()).isFalse();
        assertThat(history.messagesOldestFirst())
                .extracting(SlackChannelMessage::ts)
                .containsExactly("1700000100.000100", "1700000200.000100", "1700000300.000100");
        SlackChannelMessage second = history.messagesOldestFirst().get(1);
        assertThat(second.fileCount()).isEqualTo(2);
        assertThat(history.messagesOldestFirst().get(0).isSystemMessage()).isTrue();
        assertThat(history.messagesOldestFirst().get(2).replyCount()).isEqualTo(2);

        ClientRequest request = requests.get(0);
        assertThat(request.url().getPath()).isEqualTo("/api/conversations.history");
        assertThat(query(request, "channel")).isEqualTo("C0REPORT");
        assertThat(query(request, "oldest")).isEqualTo("1700000000.000000");
        assertThat(request.headers().getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer xoxb-test-token");
    }

    @Test
    @DisplayName("봇 글은 bot_id 를 작성자로 쓰고, 값이 없으면 null 이다")
    void botAndMissingUser() {
        SlackChannelAdapter adapter =
                adapter(
                        r ->
                                json(
                                        HttpStatus.OK,
                                        """
                                        {"ok": true, "messages": [
                                          {"ts": "2.0", "bot_id": "B1", "subtype": "bot_message", "text": "b"},
                                          {"ts": "1.0", "text": "anonymous"}]}
                                        """));

        List<SlackChannelMessage> messages =
                adapter.fetchNewerThan("C1", "0.0").messagesOldestFirst();

        assertThat(messages.get(0).user()).isNull();
        assertThat(messages.get(1).user()).isEqualTo("B1");
    }

    @Test
    @DisplayName("여러 페이지는 커서를 따라가며 모두 모으고, 오래된 순으로 합친다")
    void followsPagination() {
        SlackChannelAdapter adapter =
                adapter(
                        r -> {
                            String cursor = query(r, "cursor");
                            if (cursor == null) {
                                return json(
                                        HttpStatus.OK,
                                        """
                                        {"ok": true, "has_more": true,
                                         "response_metadata": {"next_cursor": "PAGE2"},
                                         "messages": [{"ts": "4.0", "user": "U"}, {"ts": "3.0", "user": "U"}]}
                                        """);
                            }
                            return json(
                                    HttpStatus.OK,
                                    """
                                    {"ok": true, "has_more": false,
                                     "messages": [{"ts": "2.0", "user": "U"}, {"ts": "1.0", "user": "U"}]}
                                    """);
                        });

        SlackHistory history = adapter.fetchNewerThan("C1", "0.0");

        assertThat(history.messagesOldestFirst())
                .extracting(SlackChannelMessage::ts)
                .containsExactly("1.0", "2.0", "3.0", "4.0");
        assertThat(history.truncated()).isFalse();
        assertThat(requests).hasSize(2);
        assertThat(query(requests.get(1), "cursor")).isEqualTo("PAGE2");
    }

    @Test
    @DisplayName("페이지가 안전 한도(5)를 넘으면 거기서 멈추고 truncated 로 표시한다")
    void stopsAtPageLimit() {
        SlackChannelAdapter adapter =
                adapter(
                        r ->
                                json(
                                        HttpStatus.OK,
                                        """
                                        {"ok": true, "has_more": true,
                                         "response_metadata": {"next_cursor": "MORE"},
                                         "messages": [{"ts": "9.0", "user": "U"}]}
                                        """));

        SlackHistory history = adapter.fetchNewerThan("C1", "0.0");

        assertThat(history.truncated()).isTrue();
        assertThat(requests).hasSize(SlackChannelAdapter.MAX_PAGES);
    }

    @Test
    @DisplayName("latestTs 는 limit=1 로 가장 최근 글의 ts 를 돌려주고, 글이 없으면 비어 있다")
    void latestTs() {
        SlackChannelAdapter withMessage =
                adapter(
                        r ->
                                json(
                                        HttpStatus.OK,
                                        "{\"ok\": true, \"messages\": [{\"ts\": \"7.5\", \"user\": \"U\"}]}"));
        assertThat(withMessage.latestTs("C1")).contains("7.5");
        assertThat(query(requests.get(0), "limit")).isEqualTo("1");
        assertThat(query(requests.get(0), "oldest")).isNull();

        SlackChannelAdapter empty =
                adapter(r -> json(HttpStatus.OK, "{\"ok\": true, \"messages\": []}"));
        assertThat(empty.latestTs("C1")).isEmpty();
    }

    @Test
    @DisplayName("원문 링크를 가져오고, 실패하면 비어 있다(예외를 던지지 않는다)")
    void permalink() {
        SlackChannelAdapter ok =
                adapter(
                        r ->
                                json(
                                        HttpStatus.OK,
                                        "{\"ok\": true, \"permalink\": \"https://w.slack.com/archives/C1/p1\"}"));
        assertThat(ok.permalink("C1", "1.0")).contains("https://w.slack.com/archives/C1/p1");
        assertThat(requests.get(0).url().getPath()).isEqualTo("/api/chat.getPermalink");
        assertThat(query(requests.get(0), "message_ts")).isEqualTo("1.0");

        SlackChannelAdapter failing =
                adapter(
                        r ->
                                json(
                                        HttpStatus.OK,
                                        "{\"ok\": false, \"error\": \"message_not_found\"}"));
        assertThat(failing.permalink("C1", "1.0")).isEmpty();
    }

    @Test
    @DisplayName("ok=false 이면 오류 코드와 조치 힌트가 담긴 예외를 던진다")
    void slackErrorsBecomeExceptionsWithHints() {
        assertThatThrownBy(
                        () ->
                                adapter(
                                                r ->
                                                        json(
                                                                HttpStatus.OK,
                                                                "{\"ok\": false, \"error\": \"not_in_channel\"}"))
                                        .fetchNewerThan("C1", "0.0"))
                .isInstanceOf(SlackChannelException.class)
                .hasMessageContaining("not_in_channel")
                .hasMessageContaining("초대");
        assertThatThrownBy(
                        () ->
                                adapter(
                                                r ->
                                                        json(
                                                                HttpStatus.OK,
                                                                "{\"ok\": false, \"error\": \"missing_scope\"}"))
                                        .fetchNewerThan("C1", "0.0"))
                .hasMessageContaining("channels:history");
    }

    @Test
    @DisplayName("429 는 Retry-After 를 담은 예외로, 서버 오류도 예외로 바뀐다. 토큰은 메시지에 나오지 않는다")
    void httpErrorsBecomeExceptions() {
        SlackChannelAdapter rateLimited =
                adapter(
                        r ->
                                ClientResponse.create(HttpStatus.TOO_MANY_REQUESTS)
                                        .header(HttpHeaders.RETRY_AFTER, "30")
                                        .body("{}")
                                        .build());
        assertThatThrownBy(() -> rateLimited.fetchNewerThan("C1", "0.0"))
                .isInstanceOf(SlackChannelException.class)
                .hasMessageContaining("429")
                .hasMessageContaining("30")
                .hasMessageNotContaining("xoxb-test-token");

        SlackChannelAdapter broken = adapter(r -> json(HttpStatus.INTERNAL_SERVER_ERROR, "{}"));
        assertThatThrownBy(() -> broken.fetchNewerThan("C1", "0.0"))
                .isInstanceOf(SlackChannelException.class)
                .hasMessageNotContaining("xoxb-test-token");
    }
}
