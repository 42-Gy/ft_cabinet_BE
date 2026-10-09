package com.gyeongsan.cabinet.auth.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AllowedOriginsTest {

    @Test
    @DisplayName("설정이 비어 있어도 프론트 origin 은 항상 허용된다")
    void emptyConfig_stillAllowsFrontend() {
        assertEquals(
                List.of("https://subak.site"),
                AllowedOrigins.resolve(List.of(), "https://subak.site"));
        assertEquals(
                List.of("https://subak.site"), AllowedOrigins.resolve(null, "https://subak.site"));
    }

    @Test
    @DisplayName("설정값과 프론트 origin 을 합치되 중복과 끝의 '/' 는 정리한다")
    void merge_dedupAndTrim() {
        List<String> origins =
                AllowedOrigins.resolve(
                        List.of(" https://a.example/ ", "https://subak.site"),
                        "https://subak.site");

        assertEquals(List.of("https://a.example", "https://subak.site"), origins);
    }

    @Test
    @DisplayName("빈 항목은 무시한다")
    void blankEntries_ignored() {
        List<String> origins =
                AllowedOrigins.resolve(Arrays.asList("", "  ", null), "https://subak.site");

        assertEquals(List.of("https://subak.site"), origins);
    }

    @Test
    @DisplayName("데모 설정이면 운영 도메인이나 localhost 가 코드에서 새어 들어가지 않는다")
    void demo_hasNoLegacyOrigins() {
        List<String> origins = AllowedOrigins.resolve(List.of(), "https://demo.example.com");

        assertEquals(List.of("https://demo.example.com"), origins);
    }

    @Test
    @DisplayName("프론트 URL 에 경로가 있어도 origin 만 허용한다")
    void frontendPath_originOnly() {
        assertEquals(
                List.of("https://demo.example.com"),
                AllowedOrigins.resolve(List.of(), "https://demo.example.com/app"));
    }

    @Test
    @DisplayName("프론트 URL 이 잘못되면 실패한다")
    void invalidFrontend_throws() {
        assertThrows(
                IllegalStateException.class, () -> AllowedOrigins.resolve(List.of(), "not-a-url"));
    }
}
