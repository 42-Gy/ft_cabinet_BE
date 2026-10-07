package com.gyeongsan.cabinet.adapter.out.external.kakao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoGrant;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoInsufficientScopeException;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoInvalidGrantException;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoMessage;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoNotificationException;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoRefreshedToken;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.KakaoNotificationPort;
import io.netty.channel.ChannelOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * 카카오 알림 발송용 외부 호출(인가 코드 교환, 토큰 갱신, "나에게 보내기"). 로그인용 {@link KakaoOAuthApiClientAdapter} 는 건드리지 않고
 * 따로 둔다.
 *
 * <p>토큰·인가 코드는 예외 메시지나 로그에 남기지 않는다. 오류 응답에서는 {@code error}/{@code code} 값만 읽는다.
 */
@Component
@ConditionalOnProperty(name = "app.kakao-notify.enabled", havingValue = "true")
@Log4j2
public class KakaoNotificationAdapter implements KakaoNotificationPort {

    /** 카카오 오류 코드 -402: 필요한 동의 항목이 없다. */
    static final int CODE_INSUFFICIENT_SCOPE = -402;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebClient webClient;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String clientSecret;

    @Value("${app.kakao-notify.auth-base-url:https://kauth.kakao.com}")
    private String authBaseUrl;

    @Value("${app.kakao-notify.api-base-url:https://kapi.kakao.com}")
    private String apiBaseUrl;

    public KakaoNotificationAdapter() {
        this(createWebClient());
    }

    KakaoNotificationAdapter(WebClient webClient) {
        this.webClient = webClient;
    }

    private static WebClient createWebClient() {
        HttpClient httpClient =
                HttpClient.create()
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                        .responseTimeout(Duration.ofSeconds(5));
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /** 응답 상태와 본문(JSON 객체)을 함께 읽는다. 오류 상태여도 예외 없이 본문을 돌려준다. */
    private record Reply(HttpStatusCode status, Map<String, Object> body) {
        boolean ok() {
            return status.is2xxSuccessful();
        }
    }

    @SuppressWarnings("unchecked")
    private static Mono<Reply> toReply(ClientResponse response) {
        return response.bodyToMono(Map.class)
                .defaultIfEmpty(Map.of())
                .onErrorResume(e -> Mono.just(Map.of()))
                .map(body -> new Reply(response.statusCode(), (Map<String, Object>) body));
    }

    private Reply post(String url, String bearer, MultiValueMap<String, String> form) {
        try {
            WebClient.RequestBodySpec spec =
                    webClient.post().uri(url).contentType(MediaType.APPLICATION_FORM_URLENCODED);
            if (bearer != null) {
                spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer);
            }
            Reply reply =
                    spec.body(BodyInserters.fromFormData(form))
                            .exchangeToMono(KakaoNotificationAdapter::toReply)
                            .block();
            if (reply == null) {
                throw new KakaoNotificationException("카카오 응답이 비어 있습니다.");
            }
            return reply;
        } catch (KakaoNotificationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new KakaoNotificationException(
                    "카카오 호출에 실패했습니다: " + e.getClass().getSimpleName(), e);
        }
    }

    private Reply get(String url, String bearer) {
        try {
            Reply reply =
                    webClient
                            .get()
                            .uri(url)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer)
                            .exchangeToMono(KakaoNotificationAdapter::toReply)
                            .block();
            if (reply == null) {
                throw new KakaoNotificationException("카카오 응답이 비어 있습니다.");
            }
            return reply;
        } catch (KakaoNotificationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new KakaoNotificationException(
                    "카카오 호출에 실패했습니다: " + e.getClass().getSimpleName(), e);
        }
    }

    @Override
    public KakaoGrant exchangeAuthorizationCode(String authorizationCode, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("code", authorizationCode);

        Reply token = post(authBaseUrl + "/oauth/token", null, form);
        throwIfTokenError(token, "인가 코드 교환");

        String accessToken = text(token.body().get("access_token"));
        String refreshToken = text(token.body().get("refresh_token"));
        if (accessToken == null || refreshToken == null) {
            throw new KakaoNotificationException("카카오 토큰 응답에 필요한 값이 없습니다.");
        }
        String scope = text(token.body().get("scope"));
        Set<String> scopes =
                scope == null
                        ? Set.of()
                        : Arrays.stream(scope.trim().split("\\s+"))
                                .filter(s -> !s.isEmpty())
                                .collect(Collectors.toSet());

        Reply me = get(apiBaseUrl + "/v2/user/me", accessToken);
        if (!me.ok() || me.body().get("id") == null) {
            throw new KakaoNotificationException(
                    "카카오 회원 정보를 확인하지 못했습니다(HTTP " + me.status().value() + ").");
        }
        return new KakaoGrant(String.valueOf(me.body().get("id")), refreshToken, scopes);
    }

    @Override
    public KakaoRefreshedToken refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);

        Reply reply = post(authBaseUrl + "/oauth/token", null, form);
        throwIfTokenError(reply, "토큰 갱신");

        String accessToken = text(reply.body().get("access_token"));
        if (accessToken == null) {
            throw new KakaoNotificationException("카카오 토큰 갱신 응답에 access_token 이 없습니다.");
        }
        return new KakaoRefreshedToken(accessToken, text(reply.body().get("refresh_token")));
    }

    @Override
    public void sendToMe(String accessToken, KakaoMessage message) {
        Map<String, Object> link = new LinkedHashMap<>();
        link.put("web_url", message.linkUrl());
        link.put("mobile_web_url", message.linkUrl());
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("object_type", "text");
        template.put("text", message.text());
        template.put("link", link);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        try {
            form.add("template_object", MAPPER.writeValueAsString(template));
        } catch (JsonProcessingException e) {
            throw new KakaoNotificationException("메시지 템플릿을 만들지 못했습니다.", e);
        }

        Reply reply = post(apiBaseUrl + "/v2/api/talk/memo/default/send", accessToken, form);
        if (reply.ok()) {
            Object result = reply.body().get("result_code");
            if (result == null || Integer.parseInt(String.valueOf(result)) == 0) {
                return;
            }
            throw new KakaoNotificationException("카카오 발송 결과 코드가 비정상입니다: " + result);
        }

        Integer code = intValue(reply.body().get("code"));
        if (code != null && code == CODE_INSUFFICIENT_SCOPE) {
            throw new KakaoInsufficientScopeException("talk_message 동의가 없거나 해지되었습니다.");
        }
        throw new KakaoNotificationException(
                "카카오 발송 실패(HTTP " + reply.status().value() + ", code=" + code + ")");
    }

    private void throwIfTokenError(Reply reply, String what) {
        if (reply.ok()) {
            return;
        }
        String error = text(reply.body().get("error"));
        if ("invalid_grant".equals(error)) {
            throw new KakaoInvalidGrantException(what + "이 invalid_grant 로 거절되었습니다.");
        }
        throw new KakaoNotificationException(
                what + " 실패(HTTP " + reply.status().value() + ", error=" + error + ")");
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value);
        return s.isBlank() ? null : s;
    }

    private static Integer intValue(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
