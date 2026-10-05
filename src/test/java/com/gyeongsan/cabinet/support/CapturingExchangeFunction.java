package com.gyeongsan.cabinet.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import reactor.core.publisher.Mono;

/** 실제 네트워크 없이 WebClient 요청을 가로채 기록하고, 고정된 JSON 응답을 돌려준다. */
public final class CapturingExchangeFunction implements ExchangeFunction {

    private final List<ClientRequest> requests = new ArrayList<>();
    private final String responseJson;

    public CapturingExchangeFunction(String responseJson) {
        this.responseJson = responseJson;
    }

    @Override
    public Mono<ClientResponse> exchange(ClientRequest request) {
        requests.add(request);
        return Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(responseJson)
                        .build());
    }

    public List<ClientRequest> requests() {
        return requests;
    }

    /** 요청 바디가 실제로 직렬화되어 전송될 때의 문자열(URL 인코딩 결과 포함)을 돌려준다. */
    public static String bodyOf(ClientRequest request) {
        MockClientHttpRequest mock = new MockClientHttpRequest(request.method(), request.url());
        request.body()
                .insert(
                        mock,
                        new BodyInserter.Context() {
                            @Override
                            public List<HttpMessageWriter<?>> messageWriters() {
                                return ExchangeStrategies.withDefaults().messageWriters();
                            }

                            @Override
                            public Optional<ServerHttpRequest> serverRequest() {
                                return Optional.empty();
                            }

                            @Override
                            public Map<String, Object> hints() {
                                return Collections.emptyMap();
                            }
                        })
                .block();
        return mock.getBodyAsString().block();
    }
}
