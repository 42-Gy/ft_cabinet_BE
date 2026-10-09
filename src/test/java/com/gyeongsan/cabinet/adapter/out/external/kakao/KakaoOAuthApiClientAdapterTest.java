package com.gyeongsan.cabinet.adapter.out.external.kakao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gyeongsan.cabinet.domain.auth.dto.OAuthUserInfo;
import com.gyeongsan.cabinet.support.CapturingExchangeFunction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

class KakaoOAuthApiClientAdapterTest {

    private static final String RESPONSE_JSON =
            "{\"access_token\":\"tok\",\"id\":12345,"
                    + "\"kakao_account\":{\"email\":\"kakao@example.com\"}}";

    @Test
    @DisplayName("토큰 교환 요청 바디의 값은 URL 인코딩되어 전송된다")
    void tokenRequest_valuesAreUrlEncoded() {
        CapturingExchangeFunction exchange = new CapturingExchangeFunction(RESPONSE_JSON);
        KakaoOAuthApiClientAdapter adapter =
                new KakaoOAuthApiClientAdapter(
                        WebClient.builder().exchangeFunction(exchange).build());
        ReflectionTestUtils.setField(adapter, "clientId", "client-id");
        ReflectionTestUtils.setField(adapter, "clientSecret", "se&cr=et/+");
        ReflectionTestUtils.setField(adapter, "redirectUri", "https://fallback.example/cb");

        OAuthUserInfo info =
                adapter.getOAuthUserInfo(
                        "co de&1", "https://subak.site/auth/link/callback/kakao?a=b&c=d");

        String body = CapturingExchangeFunction.bodyOf(exchange.requests().get(0));
        assertTrue(body.contains("grant_type=authorization_code"), body);
        assertTrue(body.contains("client_id=client-id"), body);
        assertTrue(body.contains("client_secret=se%26cr%3Det%2F%2B"), body);
        assertTrue(
                body.contains(
                        "redirect_uri=https%3A%2F%2Fsubak.site%2Fauth%2Flink%2Fcallback%2Fkakao"
                                + "%3Fa%3Db%26c%3Dd"),
                body);
        assertTrue(body.contains("code=co+de%261"), body);
        assertEquals("12345", info.getProviderId());
        assertEquals("kakao@example.com", info.getEmail());
    }

    @Test
    @DisplayName("redirect URI 가 비어 있으면 설정값(yml)으로 대체한다")
    void tokenRequest_blankRedirectUri_fallsBackToConfigured() {
        CapturingExchangeFunction exchange = new CapturingExchangeFunction(RESPONSE_JSON);
        KakaoOAuthApiClientAdapter adapter =
                new KakaoOAuthApiClientAdapter(
                        WebClient.builder().exchangeFunction(exchange).build());
        ReflectionTestUtils.setField(adapter, "clientId", "client-id");
        ReflectionTestUtils.setField(adapter, "clientSecret", "secret");
        ReflectionTestUtils.setField(adapter, "redirectUri", "https://fallback.example/cb");

        adapter.getOAuthUserInfo("code", " ");

        String body = CapturingExchangeFunction.bodyOf(exchange.requests().get(0));
        assertTrue(body.contains("redirect_uri=https%3A%2F%2Ffallback.example%2Fcb"), body);
    }
}
