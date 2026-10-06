package com.gyeongsan.cabinet.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.gyeongsan.cabinet.domain.user.model.FtCursusEntry;
import com.gyeongsan.cabinet.support.FtFixtures;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

/** 네트워크 없이 WebClient 교환 함수를 가로채, 요청 모양과 응답 해석을 확인한다. */
class FtApiManagerCursusTest {

    private final List<ClientRequest> requests = new ArrayList<>();

    private static ClientResponse json(HttpStatus status, String body) {
        return ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build();
    }

    private FtApiManager manager(Function<ClientRequest, ClientResponse> responder) {
        WebClient webClient =
                WebClient.builder()
                        .exchangeFunction(
                                request -> {
                                    requests.add(request);
                                    return Mono.just(responder.apply(request));
                                })
                        .build();
        FtApiManager manager = new FtApiManager(webClient);
        ReflectionTestUtils.setField(manager, "clientId", "test-id");
        ReflectionTestUtils.setField(manager, "clientSecret", "test-secret");
        ReflectionTestUtils.setField(manager, "ftApiRootUrl", "https://api.example.test");
        ReflectionTestUtils.setField(manager, "ftApiTokenUrl", "https://api.example.test/token");
        ReflectionTestUtils.setField(manager, "accessToken", "token-1");
        return manager;
    }

    @Test
    @DisplayName("/v2/users/{login} 를 Bearer 토큰으로 호출하고 cursus_users 를 해석한다")
    void fetchesAndParsesCursusEntries() {
        FtApiManager manager =
                manager(request -> json(HttpStatus.OK, FtFixtures.raw(FtFixtures.TRANSCENDER)));

        Optional<List<FtCursusEntry>> result = manager.getCursusEntries("test-user");

        assertThat(result.orElseThrow())
                .containsExactly(
                        new FtCursusEntry(9, "Pisciner"),
                        new FtCursusEntry(21, "Transcender"),
                        new FtCursusEntry(66, null));
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).url().toString())
                .isEqualTo("https://api.example.test/v2/users/test-user");
        assertThat(requests.get(0).headers().getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer token-1");
    }

    @Test
    @DisplayName("로그인명은 URI 변수로 인코딩된다: 경로 구분자나 쿼리 문자가 요청 경로를 바꾸지 못한다")
    void loginIsEncoded() {
        FtApiManager manager =
                manager(request -> json(HttpStatus.OK, FtFixtures.raw(FtFixtures.CADET)));

        manager.getCursusEntries("a/b?x=1#frag");

        assertThat(requests.get(0).url().getRawPath()).isEqualTo("/v2/users/a%2Fb%3Fx%3D1%23frag");
        assertThat(requests.get(0).url().getRawQuery()).isNull();
    }

    @Test
    @DisplayName("401 이면 토큰을 재발급해 한 번 재시도한다")
    void retriesOnceAfterUnauthorized() {
        int[] userCalls = {0};
        FtApiManager manager =
                manager(
                        request -> {
                            if (request.url().getPath().equals("/token")) {
                                return json(HttpStatus.OK, "{\"access_token\":\"token-2\"}");
                            }
                            userCalls[0]++;
                            return userCalls[0] == 1
                                    ? json(HttpStatus.UNAUTHORIZED, "{}")
                                    : json(HttpStatus.OK, FtFixtures.raw(FtFixtures.CADET));
                        });

        Optional<List<FtCursusEntry>> result = manager.getCursusEntries("test-user");

        assertThat(result).isPresent();
        assertThat(userCalls[0]).isEqualTo(2);
        assertThat(requests.get(requests.size() - 1).headers().getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer token-2");
    }

    @Test
    @DisplayName("서버 오류나 cursus_users 가 없는 응답은 빈 Optional 로 돌려준다 (예외를 던지지 않는다)")
    void failuresBecomeEmpty() {
        assertThat(
                        manager(request -> json(HttpStatus.INTERNAL_SERVER_ERROR, "{}"))
                                .getCursusEntries("test-user"))
                .isEmpty();
        assertThat(
                        manager(request -> json(HttpStatus.OK, "{\"login\":\"x\"}"))
                                .getCursusEntries("test-user"))
                .isEmpty();
    }

    @Test
    @DisplayName("재시도도 401 이면 포기하고 빈 Optional 이다")
    void giveUpAfterSecondUnauthorized() {
        FtApiManager manager =
                manager(
                        request ->
                                request.url().getPath().equals("/token")
                                        ? json(HttpStatus.OK, "{\"access_token\":\"token-2\"}")
                                        : json(HttpStatus.UNAUTHORIZED, "{}"));

        assertThat(manager.getCursusEntries("test-user")).isEmpty();
    }
}
