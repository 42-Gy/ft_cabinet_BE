package com.gyeongsan.cabinet.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

class FtApiManagerTest {

    private static WebClientResponseException responseError(int status) {
        return WebClientResponseException.create(status, "error", HttpHeaders.EMPTY, null, null);
    }

    @Test
    @DisplayName("isRetryable: retries on 429 and 5xx")
    void testRetryableStatus() {
        assertThat(FtApiManager.isRetryable(responseError(429))).isTrue();
        assertThat(FtApiManager.isRetryable(responseError(500))).isTrue();
        assertThat(FtApiManager.isRetryable(responseError(503))).isTrue();
    }

    @Test
    @DisplayName("isRetryable: does not retry on 4xx other than 429")
    void testNonRetryableStatus() {
        assertThat(FtApiManager.isRetryable(responseError(400))).isFalse();
        assertThat(FtApiManager.isRetryable(responseError(401))).isFalse();
        assertThat(FtApiManager.isRetryable(responseError(404))).isFalse();
    }

    @Test
    @DisplayName("isRetryable: retries on connection errors")
    void testRetryableConnectionError() {
        WebClientRequestException e =
                new WebClientRequestException(
                        new IOException("Connection reset by peer"),
                        HttpMethod.GET,
                        URI.create("https://api.intra.42.fr/v2/users/test/locations_stats"),
                        HttpHeaders.EMPTY);

        assertThat(FtApiManager.isRetryable(e)).isTrue();
    }

    @Test
    @DisplayName("isRetryable: does not retry on unexpected errors")
    void testNonRetryableUnexpectedError() {
        assertThat(FtApiManager.isRetryable(new IllegalStateException())).isFalse();
    }
}
