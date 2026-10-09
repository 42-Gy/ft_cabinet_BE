package com.gyeongsan.cabinet.adapter.out.external.google;

import com.gyeongsan.cabinet.domain.auth.dto.OAuthUserInfo;
import com.gyeongsan.cabinet.domain.auth.port.out.OAuthApiClientPort;
import io.netty.channel.ChannelOption;
import java.time.Duration;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Component
@Log4j2
public class GoogleOAuthApiClientAdapter implements OAuthApiClientPort {

    private final WebClient webClient;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    public GoogleOAuthApiClientAdapter() {
        this(createWebClient());
    }

    GoogleOAuthApiClientAdapter(WebClient webClient) {
        this.webClient = webClient;
    }

    private static WebClient createWebClient() {
        HttpClient httpClient =
                HttpClient.create()
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                        .responseTimeout(Duration.ofSeconds(3));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public OAuthUserInfo getOAuthUserInfo(String authorizationCode, String redirectUri) {
        String accessToken = getAccessToken(authorizationCode, redirectUri);
        return getUserProfile(accessToken);
    }

    @Override
    public boolean supports(String provider) {
        return "google".equalsIgnoreCase(provider);
    }

    @SuppressWarnings("unchecked")
    private String getAccessToken(String code, String customRedirectUri) {
        String targetRedirectUri =
                (customRedirectUri != null && !customRedirectUri.trim().isEmpty())
                        ? customRedirectUri
                        : redirectUri;

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", targetRedirectUri);
        form.add("code", code);

        Map<String, Object> response =
                webClient
                        .post()
                        .uri("https://oauth2.googleapis.com/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(BodyInserters.fromFormData(form))
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

        if (response == null || !response.containsKey("access_token")) {
            throw new IllegalArgumentException("구글 Access Token 발급에 실패했습니다.");
        }

        return (String) response.get("access_token");
    }

    @SuppressWarnings("unchecked")
    private OAuthUserInfo getUserProfile(String accessToken) {
        Map<String, Object> response =
                webClient
                        .get()
                        .uri("https://www.googleapis.com/oauth2/v3/userinfo")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

        if (response == null || response.get("sub") == null) {
            throw new IllegalArgumentException("구글 유저 정보 조회에 실패했습니다.");
        }

        String googleSubId = String.valueOf(response.get("sub"));
        String email = (String) response.get("email");

        log.info("🔍 구글 유저 정보 조회 완료: sub={}, email={}", googleSubId, email);
        return new OAuthUserInfo(googleSubId, email);
    }
}
