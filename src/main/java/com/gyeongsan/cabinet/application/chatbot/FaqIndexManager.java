package com.gyeongsan.cabinet.application.chatbot;

import com.gyeongsan.cabinet.domain.chatbot.model.EmbeddingUnavailableException;
import com.gyeongsan.cabinet.domain.chatbot.model.Faq;
import com.gyeongsan.cabinet.domain.chatbot.port.out.EmbeddingPort;
import com.gyeongsan.cabinet.domain.chatbot.port.out.FaqRepositoryPort;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.log4j.Log4j2;

/**
 * FAQ 를 DB 에서 읽어 임베딩을 계산하고 검색 색인을 만든다. 임베딩은 DB 에 저장하지 않고 메모리에서만 계산한다(수백 건이라 몇 초면 끝난다).
 *
 * <p>서버마다 각자 색인을 갖는다. 관리자가 FAQ 를 바꾸면 그 서버는 바로 다시 만들고, 다른 서버는 주기적으로 DB 의 변경 여부(fingerprint)를 보고 다시
 * 만든다. 모델을 쓸 수 없으면 준비되지 않은 상태로 남고 다음 주기에 다시 시도한다. 서버 기동이나 다른 기능은 막지 않는다.
 */
@Log4j2
public class FaqIndexManager {

    private static final String WARMUP_TEXT = "준비 확인";

    private final FaqRepositoryPort faqRepository;
    private final EmbeddingPort embedding;
    private final ReentrantLock buildLock = new ReentrantLock();
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(
                    r -> {
                        Thread t = new Thread(r, "faq-index-builder");
                        t.setDaemon(true);
                        return t;
                    });

    private volatile FaqIndex index = FaqIndex.EMPTY;
    private volatile String builtFingerprint;
    private volatile boolean ready;
    private volatile boolean failureLogged;
    private final AtomicBoolean queued = new AtomicBoolean(false);

    public FaqIndexManager(FaqRepositoryPort faqRepository, EmbeddingPort embedding) {
        this.faqRepository = faqRepository;
        this.embedding = embedding;
    }

    public boolean isReady() {
        return ready;
    }

    public FaqIndex current() {
        return index;
    }

    /** 비동기로 색인을 (다시) 만든다. 기동 직후나 관리자가 FAQ 를 바꾼 직후에 쓴다. 이미 대기 중인 요청이 있으면 합친다. */
    public void requestRebuild() {
        if (queued.compareAndSet(false, true)) {
            executor.submit(
                    () -> {
                        queued.set(false);
                        rebuildSafely();
                    });
        }
    }

    /**
     * 준비가 안 됐거나 DB 의 FAQ 가 바뀌었으면 다시 만들도록 요청한다. 주기적으로 호출한다. 확인은 가볍게(DB 한 번) 하고, 오래 걸리는 계산은 별도 스레드에서
     * 하므로 공용 스케줄러 스레드를 막지 않는다.
     */
    public void refreshIfChanged() {
        try {
            if (!ready || !faqRepository.fingerprint().equals(builtFingerprint)) {
                requestRebuild();
            }
        } catch (RuntimeException e) {
            log.warn("[Chatbot] FAQ 변경 확인 실패: {}", e.toString());
        }
    }

    private void rebuildSafely() {
        try {
            rebuild();
        } catch (EmbeddingUnavailableException e) {
            if (!failureLogged) {
                log.error("[Chatbot] 임베딩 모델을 쓸 수 없어 챗봇 색인을 만들지 못했습니다(계속 재시도): {}", e.getMessage());
                failureLogged = true;
            }
        } catch (RuntimeException e) {
            log.error("[Chatbot] 색인 생성 중 오류: {}", e.toString(), e);
        }
    }

    /** 동기 방식으로 색인을 만든다(테스트와 내부용). */
    @SuppressWarnings("UnusedReturnValue")
    public void rebuild() {
        buildLock.lock();
        try {
            // 읽기 전에 fingerprint 를 먼저 잡아야, 만드는 중에 바뀐 내용이 다음 주기에 반영된다.
            String fingerprint = faqRepository.fingerprint();

            // 모델을 미리 읽어 두어 첫 사용자 요청이 느려지지 않게 하고, 모델 문제를 여기서 드러낸다.
            embedding.embed(WARMUP_TEXT);

            List<Faq> faqs = faqRepository.findAllEnabled();
            List<FaqIndex.Entry> entries = new ArrayList<>();
            Map<Long, Faq> byId = new HashMap<>();
            long started = System.nanoTime();
            for (Faq faq : faqs) {
                byId.put(faq.id(), faq);
                for (String question : faq.questions()) {
                    String normalized = TextNormalizer.normalize(question);
                    if (!normalized.isEmpty()) {
                        entries.add(
                                new FaqIndex.Entry(
                                        faq.id(), question, embedding.embed(normalized)));
                    }
                }
            }

            this.index = new FaqIndex(entries, byId);
            this.builtFingerprint = fingerprint;
            this.ready = true;
            this.failureLogged = false;
            log.info(
                    "[Chatbot] 색인 완료: FAQ {}건, 질문 표현 {}개, 모델 {}, {}ms",
                    faqs.size(),
                    entries.size(),
                    embedding.modelId(),
                    (System.nanoTime() - started) / 1_000_000);
        } finally {
            buildLock.unlock();
        }
    }
}
