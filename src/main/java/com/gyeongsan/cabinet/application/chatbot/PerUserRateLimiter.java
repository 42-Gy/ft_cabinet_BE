package com.gyeongsan.cabinet.application.chatbot;

import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * 사용자별 요청 횟수 제한(슬라이딩 윈도). 서버 메모리에만 두므로 서버가 여러 대면 한도가 서버별로 적용된다(서버 수만큼 느슨해지는 것은 수용). 전역 한도( {@code
 * chatbotApi})와 별개로, 한 사용자가 개인 조회를 반복해 DB 를 두드리지 못하게 한다.
 *
 * <p>사용자 ID 와 요청 시각만 들고 있고, 윈도가 지난 기록은 주기적으로 지워서 메모리가 활성 사용자 수에 비례하게만 유지된다.
 */
public final class PerUserRateLimiter {

    private static final int SWEEP_EVERY_CALLS = 1024;

    private final int limit;
    private final long windowNanos;
    private final LongSupplier nanoClock;
    private final ConcurrentHashMap<Long, ArrayDeque<Long>> hits = new ConcurrentHashMap<>();
    private final AtomicLong calls = new AtomicLong();

    public PerUserRateLimiter(int limit, long windowMillis) {
        this(limit, windowMillis, System::nanoTime);
    }

    PerUserRateLimiter(int limit, long windowMillis, LongSupplier nanoClock) {
        if (limit <= 0 || windowMillis <= 0) {
            throw new IllegalArgumentException("사용자별 한도와 시간 창은 양수여야 합니다.");
        }
        this.limit = limit;
        this.windowNanos = windowMillis * 1_000_000L;
        this.nanoClock = nanoClock;
    }

    /** 한도 안이면 이번 요청을 기록하고 true, 넘었으면 기록하지 않고 false. */
    public boolean tryAcquire(long userId) {
        long now = nanoClock.getAsLong();
        boolean[] allowed = new boolean[1];
        hits.compute(
                userId,
                (id, window) -> {
                    ArrayDeque<Long> deque = window == null ? new ArrayDeque<>() : window;
                    dropExpired(deque, now);
                    if (deque.size() >= limit) {
                        return deque;
                    }
                    deque.addLast(now);
                    allowed[0] = true;
                    return deque;
                });
        if (calls.incrementAndGet() % SWEEP_EVERY_CALLS == 0) {
            sweep(now);
        }
        return allowed[0];
    }

    int trackedUsers() {
        return hits.size();
    }

    private void sweep(long now) {
        for (Long userId : hits.keySet()) {
            hits.computeIfPresent(
                    userId,
                    (id, window) -> {
                        dropExpired(window, now);
                        return window.isEmpty() ? null : window;
                    });
        }
    }

    private void dropExpired(ArrayDeque<Long> window, long now) {
        while (!window.isEmpty() && now - window.peekFirst() >= windowNanos) {
            window.pollFirst();
        }
    }
}
