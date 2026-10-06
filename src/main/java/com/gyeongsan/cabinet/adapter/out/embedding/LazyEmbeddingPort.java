package com.gyeongsan.cabinet.adapter.out.embedding;

import com.gyeongsan.cabinet.domain.chatbot.model.EmbeddingUnavailableException;
import com.gyeongsan.cabinet.domain.chatbot.port.out.EmbeddingPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * 모델을 처음 쓸 때 불러오고, 실패하면 잠시(기본 60초) 쉬었다가 다시 시도한다. 모델 파일이 없거나 네이티브 라이브러리를 못 불러와도 서버는 정상 기동하고 챗봇만 "사용할
 * 수 없음"이 된다.
 */
public class LazyEmbeddingPort implements EmbeddingPort, AutoCloseable {

    private final String modelId;
    private final Supplier<EmbeddingPort> factory;
    private final Clock clock;
    private final Duration retryBackoff;

    private EmbeddingPort delegate;
    private Instant lastFailureAt;
    private String lastFailureMessage;

    public LazyEmbeddingPort(
            String modelId, Supplier<EmbeddingPort> factory, Clock clock, Duration retryBackoff) {
        this.modelId = modelId;
        this.factory = factory;
        this.clock = clock;
        this.retryBackoff = retryBackoff;
    }

    @Override
    public String modelId() {
        return modelId;
    }

    @Override
    public float[] embed(String text) {
        return delegate().embed(text);
    }

    private synchronized EmbeddingPort delegate() {
        if (delegate != null) {
            return delegate;
        }
        if (lastFailureAt != null && clock.instant().isBefore(lastFailureAt.plus(retryBackoff))) {
            throw new EmbeddingUnavailableException(lastFailureMessage);
        }
        try {
            delegate = factory.get();
            lastFailureAt = null;
            lastFailureMessage = null;
            return delegate;
        } catch (EmbeddingUnavailableException e) {
            remember(e.getMessage());
            throw e;
        } catch (Exception | LinkageError e) {
            remember("임베딩 모델을 불러오지 못했습니다: " + e.getMessage());
            throw new EmbeddingUnavailableException(lastFailureMessage, e);
        }
    }

    private void remember(String message) {
        lastFailureAt = clock.instant();
        lastFailureMessage = message;
    }

    @Override
    public synchronized void close() throws Exception {
        if (delegate instanceof AutoCloseable closeable) {
            closeable.close();
        }
        delegate = null;
    }
}
