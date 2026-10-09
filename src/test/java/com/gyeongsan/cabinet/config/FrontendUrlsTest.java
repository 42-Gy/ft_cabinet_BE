package com.gyeongsan.cabinet.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class FrontendUrlsTest {

    @Test
    @DisplayName("운영 프론트 URL 은 그대로 통과한다")
    void requireValidBase_production() {
        assertEquals("https://subak.site", FrontendUrls.requireValidBase("https://subak.site"));
    }

    @Test
    @DisplayName("로컬 호스트는 http 를 허용한다")
    void requireValidBase_loopbackHttp() {
        assertEquals(
                "http://localhost:3000", FrontendUrls.requireValidBase("http://localhost:3000"));
        assertEquals(
                "http://127.0.0.1:5173", FrontendUrls.requireValidBase("http://127.0.0.1:5173"));
    }

    @Test
    @DisplayName("앞뒤 공백은 제거한다")
    void requireValidBase_trims() {
        assertEquals("https://subak.site", FrontendUrls.requireValidBase("  https://subak.site "));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(
            strings = {
                "",
                "   ",
                "https://subak.site/",
                "http://subak.site",
                "ftp://subak.site",
                "subak.site",
                "https://subak.site?x=1",
                "https://subak.site#frag",
                "https://user:pw@subak.site"
            })
    @DisplayName("비어 있거나 형식·보안 규칙에 어긋나면 부팅 단계에서 실패한다")
    void requireValidBase_invalid(String url) {
        assertThrows(IllegalStateException.class, () -> FrontendUrls.requireValidBase(url));
    }

    @Test
    @DisplayName("origin 은 scheme://host[:port] 만 남긴다")
    void originOf() {
        assertEquals("https://subak.site", FrontendUrls.originOf("https://subak.site"));
        assertEquals(
                "https://demo.example.com:8443",
                FrontendUrls.originOf("https://demo.example.com:8443/app"));
        assertEquals("http://localhost:3000", FrontendUrls.originOf("http://localhost:3000"));
    }
}
