package com.gyeongsan.cabinet.adapter.out.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class FrontendLinkRedirectUriAdapterTest {

    @Test
    @DisplayName("운영 설정이면 외부화 이전의 하드코딩 값과 정확히 같은 URI 가 나온다")
    void production_matchesLegacyConstants() {
        FrontendLinkRedirectUriAdapter adapter =
                new FrontendLinkRedirectUriAdapter("https://subak.site");

        assertEquals(
                "https://subak.site/auth/link/callback/kakao", adapter.getLinkRedirectUri("kakao"));
        assertEquals(
                "https://subak.site/auth/link/callback/google",
                adapter.getLinkRedirectUri("google"));
    }

    @Test
    @DisplayName("provider 는 대소문자를 구분하지 않는다")
    void provider_caseInsensitive() {
        FrontendLinkRedirectUriAdapter adapter =
                new FrontendLinkRedirectUriAdapter("https://subak.site");

        assertEquals(
                "https://subak.site/auth/link/callback/kakao", adapter.getLinkRedirectUri("Kakao"));
    }

    @Test
    @DisplayName("데모 도메인이면 운영 도메인이 섞이지 않고 데모 도메인으로 만들어진다")
    void demo_usesDemoOrigin() {
        FrontendLinkRedirectUriAdapter adapter =
                new FrontendLinkRedirectUriAdapter("https://demo.example.com");

        assertEquals(
                "https://demo.example.com/auth/link/callback/google",
                adapter.getLinkRedirectUri("google"));
    }

    @Test
    @DisplayName("프론트 URL 에 경로가 있으면 그 경로 아래에 붙는다")
    void basePath_isPreserved() {
        FrontendLinkRedirectUriAdapter adapter =
                new FrontendLinkRedirectUriAdapter("https://demo.example.com/app");

        assertEquals(
                "https://demo.example.com/app/auth/link/callback/kakao",
                adapter.getLinkRedirectUri("kakao"));
    }

    @Test
    @DisplayName("로컬 개발 URL 도 만들어진다")
    void local_http() {
        FrontendLinkRedirectUriAdapter adapter =
                new FrontendLinkRedirectUriAdapter("http://localhost:3000");

        assertEquals(
                "http://localhost:3000/auth/link/callback/kakao",
                adapter.getLinkRedirectUri("kakao"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  ", "../x", "kakao/../x", "kakao?x=1", "카카오", "1kakao", "a b"})
    @DisplayName("provider 에 경로·쿼리 문자가 섞여 있으면 거부한다")
    void provider_invalid(String provider) {
        FrontendLinkRedirectUriAdapter adapter =
                new FrontendLinkRedirectUriAdapter("https://subak.site");

        assertThrows(IllegalArgumentException.class, () -> adapter.getLinkRedirectUri(provider));
    }

    @Test
    @DisplayName("프론트 URL 이 잘못되면 생성 시점(부팅)에 실패한다")
    void invalidFrontendUrl_failsFast() {
        assertThrows(
                IllegalStateException.class,
                () -> new FrontendLinkRedirectUriAdapter("https://subak.site/"));
        assertThrows(
                IllegalStateException.class,
                () -> new FrontendLinkRedirectUriAdapter("http://subak.site"));
    }
}
