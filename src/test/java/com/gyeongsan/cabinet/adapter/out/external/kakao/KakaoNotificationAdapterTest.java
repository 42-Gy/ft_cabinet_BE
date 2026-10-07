package com.gyeongsan.cabinet.adapter.out.external.kakao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoGrant;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoInsufficientScopeException;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoInvalidGrantException;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoMessage;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoNotificationException;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoRefreshedToken;
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

/** 실제 네트워크 없이 카카오 응답(합성 데이터)을 흉내 내 요청 모양과 오류 해석을 확인한다. */
class KakaoNotificationAdapterTest {

    private final List<ClientRequest> requests = new ArrayList<>();
    private final List<String> bodies = new ArrayList<>();

    private static ClientResponse json(HttpStatus status, String body) {
        return ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build();
    }

    private KakaoNotificationAdapter adapter(Function<ClientRequest, ClientResponse> responder) {
        WebClient client =
                WebClient.builder()
                        .exchangeFunction(
                                request -> {
                                    requests.add(request);
                                    return Mono.just(responder.apply(request));
                                })
                        .build();
        KakaoNotificationAdapter adapter = new KakaoNotificationAdapter(client);
        ReflectionTestUtils.setField(adapter, "clientId", "client-id");
        ReflectionTestUtils.setField(adapter, "clientSecret", "client-secret");
        ReflectionTestUtils.setField(adapter, "authBaseUrl", "https://kauth.example.test");
        ReflectionTestUtils.setField(adapter, "apiBaseUrl", "https://kapi.example.test");
        return adapter;
    }

    /** 보낸 요청의 form 본문을 읽는다. */
    private static String formBody(ClientRequest request) {
        org.springframework.mock.http.client.reactive.MockClientHttpRequest mock =
                new org.springframework.mock.http.client.reactive.MockClientHttpRequest(
                        org.springframework.http.HttpMethod.POST, request.url());
        request.body()
                .insert(
                        mock,
                        new org.springframework.web.reactive.function.BodyInserter.Context() {
                            @Override
                            public List<org.springframework.http.codec.HttpMessageWriter<?>>
                                    messageWriters() {
                                return org.springframework.web.reactive.function.server
                                        .HandlerStrategies.withDefaults()
                                        .messageWriters();
                            }

                            @Override
                            public java.util.Optional<
                                            org.springframework.http.server.reactive
                                                    .ServerHttpRequest>
                                    serverRequest() {
                                return java.util.Optional.empty();
                            }

                            @Override
                            public java.util.Map<String, Object> hints() {
                                return java.util.Map.of();
                            }
                        })
                .block();
        return java.net.URLDecoder.decode(
                mock.getBodyAsString().block(), java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String path(ClientRequest request) {
        return request.url().getPath();
    }

    @Test
    @DisplayName("인가 코드를 교환하고 회원 번호와 동의 범위를 돌려준다(토큰은 반환값 문자열에 나오지 않는다)")
    void exchangesCode() {
        KakaoNotificationAdapter adapter =
                adapter(
                        request ->
                                path(request).equals("/oauth/token")
                                        ? json(
                                                HttpStatus.OK,
                                                "{\"access_token\":\"AT\",\"refresh_token\":\"RT\","
                                                        + "\"scope\":\"profile_nickname talk_message\"}")
                                        : json(HttpStatus.OK, "{\"id\":12345}"));

        KakaoGrant grant =
                adapter.exchangeAuthorizationCode("the-code", "https://front/link/kakao");

        assertThat(grant.providerId()).isEqualTo("12345");
        assertThat(grant.refreshToken()).isEqualTo("RT");
        assertThat(grant.scopes()).containsExactlyInAnyOrder("profile_nickname", "talk_message");
        assertThat(grant.toString()).doesNotContain("RT").doesNotContain("AT");
        assertThat(requests.get(0).url().toString())
                .isEqualTo("https://kauth.example.test/oauth/token");
        assertThat(formBody(requests.get(0)))
                .contains("grant_type=authorization_code")
                .contains("code=the-code")
                .contains("redirect_uri=https://front/link/kakao")
                .contains("client_id=client-id");
        assertThat(
                        requests.stream()
                                .filter(r -> path(r).equals("/v2/user/me"))
                                .findFirst()
                                .orElseThrow()
                                .headers()
                                .getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer AT");
    }

    @Test
    @DisplayName("인가 코드 교환에서 invalid_grant 면 KakaoInvalidGrantException, 그 밖의 오류는 일반 실패로 구분한다")
    void exchangeErrors() {
        KakaoNotificationAdapter expired =
                adapter(
                        request ->
                                json(
                                        HttpStatus.BAD_REQUEST,
                                        "{\"error\":\"invalid_grant\",\"error_code\":\"KOE320\"}"));
        assertThatThrownBy(() -> expired.exchangeAuthorizationCode("c", "r"))
                .isInstanceOf(KakaoInvalidGrantException.class);

        KakaoNotificationAdapter other =
                adapter(request -> json(HttpStatus.BAD_REQUEST, "{\"error\":\"invalid_client\"}"));
        assertThatThrownBy(() -> other.exchangeAuthorizationCode("c", "r"))
                .isInstanceOf(KakaoNotificationException.class)
                .isNotInstanceOf(KakaoInvalidGrantException.class);

        KakaoNotificationAdapter noRefresh =
                adapter(request -> json(HttpStatus.OK, "{\"access_token\":\"AT\"}"));
        assertThatThrownBy(() -> noRefresh.exchangeAuthorizationCode("c", "r"))
                .isInstanceOf(KakaoNotificationException.class);
    }

    @Test
    @DisplayName("토큰 갱신은 refresh_token 을 보내고, 회전된 새 refresh_token 이 있으면 함께 돌려준다")
    void refreshes() {
        KakaoNotificationAdapter adapter =
                adapter(
                        request ->
                                json(
                                        HttpStatus.OK,
                                        "{\"access_token\":\"NEW-AT\",\"refresh_token\":\"NEW-RT\"}"));

        KakaoRefreshedToken token = adapter.refreshAccessToken("OLD-RT");

        assertThat(token.accessToken()).isEqualTo("NEW-AT");
        assertThat(token.newRefreshToken()).isEqualTo("NEW-RT");
        assertThat(token.toString()).doesNotContain("NEW-");
        assertThat(formBody(requests.get(0)))
                .contains("grant_type=refresh_token")
                .contains("refresh_token=OLD-RT");

        KakaoNotificationAdapter noRotation =
                adapter(request -> json(HttpStatus.OK, "{\"access_token\":\"AT2\"}"));
        assertThat(noRotation.refreshAccessToken("RT").newRefreshToken()).isNull();
    }

    @Test
    @DisplayName("토큰 갱신이 invalid_grant 면 동의 해지로 해석하고, 서버 오류는 일시적 실패로 둔다")
    void refreshErrors() {
        KakaoNotificationAdapter revoked =
                adapter(request -> json(HttpStatus.BAD_REQUEST, "{\"error\":\"invalid_grant\"}"));
        assertThatThrownBy(() -> revoked.refreshAccessToken("RT"))
                .isInstanceOf(KakaoInvalidGrantException.class);

        KakaoNotificationAdapter down =
                adapter(request -> json(HttpStatus.SERVICE_UNAVAILABLE, "{}"));
        assertThatThrownBy(() -> down.refreshAccessToken("RT"))
                .isInstanceOf(KakaoNotificationException.class)
                .isNotInstanceOf(KakaoInvalidGrantException.class);
    }

    @Test
    @DisplayName("메시지는 텍스트 템플릿(본문과 링크)으로 보낸다")
    void sends() {
        KakaoNotificationAdapter adapter =
                adapter(request -> json(HttpStatus.OK, "{\"result_code\":0}"));

        adapter.sendToMe("AT", new KakaoMessage("📢 [공지] 안내", "https://front.example"));

        ClientRequest sent = requests.get(0);
        assertThat(sent.url().toString())
                .isEqualTo("https://kapi.example.test/v2/api/talk/memo/default/send");
        assertThat(sent.headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer AT");
        assertThat(sent.headers().getContentType())
                .isEqualTo(MediaType.APPLICATION_FORM_URLENCODED);
        String body = formBody(sent);
        assertThat(body).startsWith("template_object=");
        com.fasterxml.jackson.databind.JsonNode template;
        try {
            template =
                    new com.fasterxml.jackson.databind.ObjectMapper()
                            .readTree(body.substring("template_object=".length()));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        assertThat(template.path("object_type").asText()).isEqualTo("text");
        assertThat(template.path("text").asText()).isEqualTo("📢 [공지] 안내");
        assertThat(template.path("link").path("web_url").asText())
                .isEqualTo("https://front.example");
        assertThat(template.path("link").path("mobile_web_url").asText())
                .isEqualTo("https://front.example");
    }

    @Test
    @DisplayName("발송 오류 코드 -402 는 동의 항목 부족으로, 그 밖의 오류는 일반 실패로 구분한다")
    void sendErrors() {
        KakaoNotificationAdapter noScope =
                adapter(
                        request ->
                                json(
                                        HttpStatus.FORBIDDEN,
                                        "{\"msg\":\"insufficient scopes\",\"code\":-402}"));
        assertThatThrownBy(() -> noScope.sendToMe("AT", new KakaoMessage("x", "https://f")))
                .isInstanceOf(KakaoInsufficientScopeException.class);

        KakaoNotificationAdapter badToken =
                adapter(
                        request ->
                                json(
                                        HttpStatus.UNAUTHORIZED,
                                        "{\"msg\":\"invalid token\",\"code\":-401}"));
        assertThatThrownBy(() -> badToken.sendToMe("AT", new KakaoMessage("x", "https://f")))
                .isInstanceOf(KakaoNotificationException.class)
                .isNotInstanceOf(KakaoInsufficientScopeException.class)
                .hasMessageNotContaining("AT");
    }

    @Test
    @DisplayName("네트워크 오류도 일반 실패로 감싸 던지고, 메시지에 토큰이 들어가지 않는다")
    void networkFailure() {
        WebClient failing =
                WebClient.builder()
                        .exchangeFunction(
                                request -> Mono.error(new java.io.IOException("connection reset")))
                        .build();
        KakaoNotificationAdapter adapter = new KakaoNotificationAdapter(failing);
        ReflectionTestUtils.setField(adapter, "authBaseUrl", "https://kauth.example.test");
        ReflectionTestUtils.setField(adapter, "apiBaseUrl", "https://kapi.example.test");

        assertThatThrownBy(() -> adapter.refreshAccessToken("SECRET-RT"))
                .isInstanceOf(KakaoNotificationException.class)
                .hasMessageNotContaining("SECRET-RT");
    }
}
