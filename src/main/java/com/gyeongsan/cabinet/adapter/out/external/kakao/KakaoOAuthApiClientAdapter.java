package com.gyeongsan.cabinet.adapter.out.external.kakao;

import com.gyeongsan.cabinet.domain.auth.dto.OAuthUserInfo;
import com.gyeongsan.cabinet.domain.auth.port.out.OAuthApiClientPort;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import io.netty.channel.ChannelOption;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;
import java.time.Duration;

import java.util.Map;

@Component
@Log4j2
public class KakaoOAuthApiClientAdapter implements OAuthApiClientPort {

    private final WebClient webClient;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String redirectUri;

    public KakaoOAuthApiClientAdapter() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .responseTimeout(Duration.ofSeconds(3));

        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public OAuthUserInfo getOAuthUserInfo(String authorizationCode) {
        String accessToken = getAccessToken(authorizationCode);
        return getUserProfile(accessToken);
    }

    @Override
    public boolean supports(String provider) {
        return "kakao".equalsIgnoreCase(provider);
    }

    @SuppressWarnings("unchecked")
    private String getAccessToken(String code) {
        Map<String, Object> response = webClient.post()
                .uri("https://kauth.kakao.com/oauth/token")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .bodyValue("grant_type=authorization_code"
                        + "&client_id=" + clientId
                        + "&client_secret=" + clientSecret
                        + "&redirect_uri=" + redirectUri
                        + "&code=" + code)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("access_token")) {
            throw new IllegalArgumentException("카카오 Access Token 발급에 실패했습니다.");
        }

        return (String) response.get("access_token");
    }

    @SuppressWarnings("unchecked")
    private OAuthUserInfo getUserProfile(String accessToken) {
        Map<String, Object> response = webClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || response.get("id") == null) {
            throw new IllegalArgumentException("카카오 유저 정보 조회에 실패했습니다.");
        }

        String kakaoId = String.valueOf(response.get("id"));

        String email = null;
        Map<String, Object> kakaoAccount = (Map<String, Object>) response.get("kakao_account");
        if (kakaoAccount != null) {
            email = (String) kakaoAccount.get("email");
        }

        log.info("🔍 카카오 유저 정보 조회 완료: id={}, email={}", kakaoId, email);
        return new OAuthUserInfo(kakaoId, email);
    }
}
