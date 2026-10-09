package com.gyeongsan.cabinet.common.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import com.gyeongsan.cabinet.support.RedisTestSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 실제 Redis 로 분산락의 상호 배제, 소유자 확인 해제, 경합 시 응답을 확인한다. Docker 가 없으면 건너뛴다. */
@Testcontainers(disabledWithoutDocker = true)
class DistributedLockAopRedisTest {

    private static GenericContainer<?> redis;
    private static LettuceConnectionFactory factory;
    private static StringRedisTemplate template;

    @BeforeAll
    static void start() {
        redis = RedisTestSupport.newRedis();
        redis.start();
        factory = RedisTestSupport.connectionFactory(redis);
        template = RedisTestSupport.template(factory);
    }

    @AfterAll
    static void stop() {
        if (factory != null) {
            factory.destroy();
        }
        if (redis != null) {
            redis.stop();
        }
    }

    @BeforeEach
    void flush() {
        template.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    /** 락 키는 모두 같은 고정 키. lease 2초, 대기 1초. 시각은 객체 생성 기준 ms 다. */
    public static class Target {
        final long t0 = System.currentTimeMillis();
        final AtomicInteger active = new AtomicInteger();
        final AtomicInteger maxActive = new AtomicInteger();
        final List<String> events = new CopyOnWriteArrayList<>();
        volatile long bEnd = -1;
        volatile long cStart = -1;

        @DistributedLock(key = "test-lock", leaseTime = 2, waitTime = 1)
        public void run(String id, long holdMs) throws InterruptedException {
            long start = System.currentTimeMillis() - t0;
            if (id.equals("C")) cStart = start;
            maxActive.accumulateAndGet(active.incrementAndGet(), Math::max);
            events.add(start + "ms " + id + " 진입");
            try {
                Thread.sleep(holdMs);
            } finally {
                long end = System.currentTimeMillis() - t0;
                if (id.equals("B")) bEnd = end;
                events.add(end + "ms " + id + " 종료");
                active.decrementAndGet();
            }
        }

        @DistributedLock(key = "test-lock", leaseTime = 2, waitTime = 1)
        public void fail() {
            throw new IllegalArgumentException("business failure");
        }
    }

    private static Target proxy(Target target) {
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
        proxyFactory.addAspect(new DistributedLockAop(template));
        return proxyFactory.getProxy();
    }

    @Test
    @DisplayName("같은 키는 동시에 하나만 실행되고, 끝나면 락 키가 사라진다")
    void mutualExclusion() throws Exception {
        Target target = new Target();
        Target proxy = proxy(target);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            CyclicBarrier barrier = new CyclicBarrier(4);
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                futures.add(
                        pool.submit(
                                () -> {
                                    barrier.await();
                                    // 락이 잠깐(100ms)만 잡히므로 대기 1초 안에 순서대로 모두 통과한다.
                                    proxy.run("same", 100);
                                    return null;
                                }));
            }
            for (Future<?> f : futures) {
                f.get();
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(target.maxActive.get()).isEqualTo(1);
        assertThat(template.hasKey("test-lock")).isFalse();
    }

    @Test
    @DisplayName("업무 로직이 예외로 끝나도 락은 풀린다")
    void releasedAfterBusinessFailure() {
        Target proxy = proxy(new Target());

        assertThatThrownBy(proxy::fail).isInstanceOf(IllegalArgumentException.class);

        assertThat(template.hasKey("test-lock")).isFalse();
    }

    @Test
    @DisplayName("락을 못 얻으면 409(REQUEST_IN_PROGRESS)로 거부하고, 먼저 잡은 요청의 락은 건드리지 않는다")
    void contentionIsRejectedWithConflict() throws Exception {
        Target proxy = proxy(new Target());
        ExecutorService pool = Executors.newFixedThreadPool(1);
        try {
            Future<?> holder =
                    pool.submit(
                            () -> {
                                proxy.run("holder", 1800);
                                return null;
                            });
            Thread.sleep(300);

            assertThatThrownBy(() -> proxy.run("rejected", 10))
                    .isInstanceOfSatisfying(
                            ServiceException.class,
                            e -> {
                                assertThat(e.getErrorCode())
                                        .isEqualTo(ErrorCode.REQUEST_IN_PROGRESS);
                                assertThat(e.getErrorCode().getStatus().value()).isEqualTo(409);
                            });
            // 거부당한 요청이 먼저 잡은 요청의 락을 지우면 안 된다.
            assertThat(template.hasKey("test-lock")).isTrue();

            holder.get();
        } finally {
            pool.shutdownNow();
        }
        assertThat(template.hasKey("test-lock")).isFalse();
    }

    @Test
    @DisplayName("lease 가 만료된 뒤 먼저 끝난 요청이 다음 요청의 락을 지우지 않는다 (소유자 확인 해제)")
    void expiredHolderDoesNotReleaseNextHoldersLock() throws Exception {
        // lease 2초. A: 0~3.2초, B: 2.3~3.8초, C: A 가 끝난 직후(3.2초) 시도.
        //  - A 의 락은 2.0초에 만료 -> B 가 2.3초에 락을 잡는다(A 와 B 가 겹치는 것은 lease 만료 자체의 결과다).
        //  - A 가 3.2초에 끝나며 해제를 시도한다. 소유자를 확인하지 않으면 B 의 락을 지워 C 가 B 와 겹쳐 진입한다.
        //  - 소유자를 확인하면 B 의 락이 유지되어, C 는 B 가 끝난 뒤에야 진입한다.
        Target target = new Target();
        Target proxy = proxy(target);
        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            Future<?> a =
                    pool.submit(
                            () -> {
                                proxy.run("A", 3200);
                                return null;
                            });
            Thread.sleep(2300);
            Future<?> b =
                    pool.submit(
                            () -> {
                                proxy.run("B", 1500);
                                return null;
                            });
            a.get();
            // A 가 끝난 직후에도 B 가 잡은 락은 남아 있어야 한다.
            assertThat(template.hasKey("test-lock"))
                    .as("A 의 해제가 B 의 락을 지웠다: %s", target.events)
                    .isTrue();
            Future<?> c =
                    pool.submit(
                            () -> {
                                proxy.run("C", 100);
                                return null;
                            });
            b.get();
            c.get();
        } finally {
            pool.shutdownNow();
        }

        assertThat(target.bEnd).isPositive();
        assertThat(target.cStart)
                .as("C 는 B 가 끝난 뒤에 진입해야 한다: %s", target.events)
                .isGreaterThanOrEqualTo(target.bEnd);
        assertThat(template.hasKey("test-lock")).isFalse();
    }
}
