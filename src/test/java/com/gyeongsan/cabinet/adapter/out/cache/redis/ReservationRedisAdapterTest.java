package com.gyeongsan.cabinet.adapter.out.cache.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.gyeongsan.cabinet.domain.lent.model.ReservationOutcome;
import com.gyeongsan.cabinet.support.RedisTestSupport;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 예약 두 키(사물함 키, 유저 키)가 항상 함께 바뀌는지 실제 Redis 에서 확인한다. Docker 가 없으면 건너뛴다. */
@Testcontainers(disabledWithoutDocker = true)
class ReservationRedisAdapterTest {

    private static GenericContainer<?> redis;
    private static LettuceConnectionFactory factory;
    private static StringRedisTemplate template;
    private static ReservationRedisAdapter adapter;

    @BeforeAll
    static void startRedis() {
        redis = RedisTestSupport.newRedis();
        redis.start();
        factory = RedisTestSupport.connectionFactory(redis);
        template = RedisTestSupport.template(factory);
        adapter = new ReservationRedisAdapter(template);
    }

    @AfterAll
    static void stopRedis() {
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

    private static void assertHolds(Long userId, Integer visibleNum) {
        assertThat(adapter.getReservedUserId(visibleNum)).contains(userId);
        assertThat(adapter.getUserReservation(userId)).contains(visibleNum);
    }

    @Test
    @DisplayName("새 예약은 두 키를 함께 만들고 15분 TTL 을 둔다")
    void reserveReplacing_new() {
        ReservationOutcome outcome = adapter.reserveReplacing(101, 7L, 15);

        assertThat(outcome.status()).isEqualTo(ReservationOutcome.Status.RESERVED);
        assertThat(outcome.replacedVisibleNum()).isNull();
        assertHolds(7L, 101);
        assertThat(template.getExpire("cabinet:reservation:101")).isBetween(890L, 900L);
        assertThat(adapter.getUserReservationTtlSeconds(7L).orElseThrow()).isBetween(890L, 900L);
    }

    @Test
    @DisplayName("다른 사물함을 예약하면 기존 예약을 자동 취소하고 새로 예약한다(방안 A)")
    void reserveReplacing_replacesOldReservation() {
        adapter.reserveReplacing(101, 7L, 15);

        ReservationOutcome outcome = adapter.reserveReplacing(102, 7L, 15);

        assertThat(outcome.status()).isEqualTo(ReservationOutcome.Status.RESERVED);
        assertThat(outcome.replacedVisibleNum()).isEqualTo(101);
        assertHolds(7L, 102);
        assertThat(adapter.getReservedUserId(101)).isEmpty();
    }

    @Test
    @DisplayName("같은 사물함을 다시 예약하면 거부하고, TTL 도 갱신하지 않는다")
    void reserveReplacing_sameCabinet_isRejectedWithoutRefreshingTtl() throws Exception {
        adapter.reserveReplacing(101, 7L, 15);
        template.expire("cabinet:reservation:101", java.time.Duration.ofSeconds(100));
        template.expire("user:reservation:7", java.time.Duration.ofSeconds(100));

        ReservationOutcome outcome = adapter.reserveReplacing(101, 7L, 15);

        assertThat(outcome.status()).isEqualTo(ReservationOutcome.Status.ALREADY_MINE);
        assertThat(adapter.getUserReservationTtlSeconds(7L).orElseThrow())
                .isLessThanOrEqualTo(100L);
        assertHolds(7L, 101);
    }

    @Test
    @DisplayName("다른 사용자가 예약한 사물함은 거부하고, 내 기존 예약도 건드리지 않는다")
    void reserveReplacing_takenByOther_changesNothing() {
        adapter.reserveReplacing(101, 7L, 15);
        adapter.reserveReplacing(102, 8L, 15);

        ReservationOutcome outcome = adapter.reserveReplacing(102, 7L, 15);

        assertThat(outcome.status()).isEqualTo(ReservationOutcome.Status.TAKEN_BY_OTHER);
        assertHolds(7L, 101);
        assertHolds(8L, 102);
    }

    @Test
    @DisplayName("유령 선점 회귀: 101 을 예약하고 102 를 대여하면 101 예약도 함께 정리된다")
    void deleteReservation_clearsReservationOnAnotherCabinet() {
        adapter.reserveReplacing(101, 7L, 15);

        adapter.deleteReservation(102, 7L);

        assertThat(adapter.getReservedUserId(101)).isEmpty();
        assertThat(adapter.getUserReservation(7L)).isEmpty();
    }

    @Test
    @DisplayName("대여한 사물함을 예약했던 경우에도 두 키가 모두 지워진다")
    void deleteReservation_rentedCabinetWasReserved() {
        adapter.reserveReplacing(101, 7L, 15);

        adapter.deleteReservation(101, 7L);

        assertThat(adapter.getReservedUserId(101)).isEmpty();
        assertThat(adapter.getUserReservation(7L)).isEmpty();
    }

    @Test
    @DisplayName("남의 예약은 지우지 않는다(소유자 확인)")
    void deleteReservation_doesNotTouchOthersReservation() {
        adapter.reserveReplacing(102, 8L, 15);

        adapter.deleteReservation(102, 7L);

        assertHolds(8L, 102);
    }

    @Test
    @DisplayName("예약 취소는 취소한 번호를 돌려주고, 두 키를 모두 지운다")
    void cancelUserReservation() {
        adapter.reserveReplacing(101, 7L, 15);

        assertThat(adapter.cancelUserReservation(7L)).contains(101);

        assertThat(adapter.getReservedUserId(101)).isEmpty();
        assertThat(adapter.getUserReservation(7L)).isEmpty();
        assertThat(adapter.cancelUserReservation(7L)).isEmpty();
    }

    @Test
    @DisplayName("예약이 없으면 남은 시간도 없다")
    void ttl_absentWhenNoReservation() {
        assertThat(adapter.getUserReservationTtlSeconds(7L)).isEmpty();
    }

    @Test
    @DisplayName("과거 버그로 사물함 키만 남은 유령 예약은, 같은 사용자의 새 예약으로 정상으로 복구된다")
    void reserveReplacing_healsOwnGhostKey() {
        template.opsForValue().set("cabinet:reservation:101", "7");

        ReservationOutcome outcome = adapter.reserveReplacing(101, 7L, 15);

        assertThat(outcome.status()).isEqualTo(ReservationOutcome.Status.RESERVED);
        assertHolds(7L, 101);
    }

    @Test
    @DisplayName("같은 사용자가 서로 다른 사물함을 동시에 예약해도 예약은 항상 하나만 남는다")
    void reserveReplacing_concurrentDifferentCabinets_leavesExactlyOne() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 300; i++) {
                flush();
                CyclicBarrier barrier = new CyclicBarrier(2);
                Future<?> a = pool.submit(() -> reserveAfterBarrier(barrier, 201));
                Future<?> b = pool.submit(() -> reserveAfterBarrier(barrier, 202));
                a.get();
                b.get();

                int held =
                        (adapter.getReservedUserId(201).isPresent() ? 1 : 0)
                                + (adapter.getReservedUserId(202).isPresent() ? 1 : 0);
                assertThat(held).as("반복 %d: 사물함 키 수", i).isEqualTo(1);
                Integer mine = adapter.getUserReservation(9L).orElseThrow();
                assertThat(adapter.getReservedUserId(mine)).as("반복 %d", i).contains(9L);
            }
        } finally {
            pool.shutdown();
        }
    }

    private static void reserveAfterBarrier(CyclicBarrier barrier, int cabinet) {
        try {
            barrier.await();
            adapter.reserveReplacing(cabinet, 9L, 15);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("서로 다른 사용자가 같은 사물함을 동시에 예약하면 한 명만 성공한다")
    void reserveReplacing_concurrentSameCabinet_onlyOneWins() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 200; i++) {
                flush();
                CyclicBarrier barrier = new CyclicBarrier(2);
                Future<ReservationOutcome> a =
                        pool.submit(() -> reserveUserAfterBarrier(barrier, 301, 11L));
                Future<ReservationOutcome> b =
                        pool.submit(() -> reserveUserAfterBarrier(barrier, 301, 12L));
                List<ReservationOutcome.Status> results =
                        List.of(a.get().status(), b.get().status());

                assertThat(results)
                        .as("반복 %d", i)
                        .containsExactlyInAnyOrder(
                                ReservationOutcome.Status.RESERVED,
                                ReservationOutcome.Status.TAKEN_BY_OTHER);
            }
        } finally {
            pool.shutdown();
        }
    }

    private static ReservationOutcome reserveUserAfterBarrier(
            CyclicBarrier barrier, int cabinet, Long userId) {
        try {
            barrier.await();
            return adapter.reserveReplacing(cabinet, userId, 15);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
