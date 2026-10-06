package com.gyeongsan.cabinet.application.lent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gyeongsan.cabinet.adapter.out.cache.redis.ReservationRedisAdapter;
import com.gyeongsan.cabinet.domain.cabinet.model.Cabinet;
import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import com.gyeongsan.cabinet.domain.cabinet.model.LentType;
import com.gyeongsan.cabinet.domain.cabinet.port.out.CabinetRepositoryPort;
import com.gyeongsan.cabinet.domain.item.model.ItemHistory;
import com.gyeongsan.cabinet.domain.item.model.ItemType;
import com.gyeongsan.cabinet.domain.item.port.out.ItemHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.model.LentHistory;
import com.gyeongsan.cabinet.domain.lent.port.out.AiCheckPort;
import com.gyeongsan.cabinet.domain.lent.port.out.ImageUploadPort;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.ReservationPort;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import com.gyeongsan.cabinet.support.RedisTestSupport;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 실제 LentApplicationService 와 실제 Redis 로 예약 흐름을 확인한다. 나머지 의존은 목이다. 분산락 AOP 는 스프링 없이 직접 호출하므로 적용되지
 * 않으며, 그래서 이 테스트는 락이 없어도 Redis 쪽 원자성만으로 예약이 하나만 남는다는 것을 보여 준다. Docker 가 없으면 건너뛴다.
 */
@Testcontainers(disabledWithoutDocker = true)
class LentReservationRedisTest {

    private static final Long USER_ID = 9L;

    private static GenericContainer<?> redis;
    private static LettuceConnectionFactory factory;
    private static StringRedisTemplate template;

    private ReservationPort reservationPort;
    private UserRepositoryPort userRepository;
    private CabinetRepositoryPort cabinetRepository;
    private LentRepositoryPort lentRepository;
    private ItemHistoryRepositoryPort itemHistoryRepository;
    private LentApplicationService service;

    @BeforeAll
    static void startRedis() {
        redis = RedisTestSupport.newRedis();
        redis.start();
        factory = RedisTestSupport.connectionFactory(redis);
        template = RedisTestSupport.template(factory);
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
    void setUp() {
        template.getConnectionFactory().getConnection().serverCommands().flushAll();

        reservationPort = new ReservationRedisAdapter(template);
        userRepository = mock(UserRepositoryPort.class);
        cabinetRepository = mock(CabinetRepositoryPort.class);
        lentRepository = mock(LentRepositoryPort.class);
        itemHistoryRepository = mock(ItemHistoryRepositoryPort.class);
        service =
                new LentApplicationService(
                        userRepository,
                        cabinetRepository,
                        lentRepository,
                        itemHistoryRepository,
                        reservationPort,
                        mock(AiCheckPort.class),
                        mock(ImageUploadPort.class),
                        mock(TransactionTemplate.class));
        ReflectionTestUtils.setField(service, "lentTerm", 31);
        ReflectionTestUtils.setField(service, "extensionTerm", 3L);

        User user = user(USER_ID, "intra09");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(lentRepository.findByUserIdAndEndedAtIsNull(USER_ID)).thenReturn(Optional.empty());
        for (int n : new int[] {101, 102, 201, 202}) {
            availableCabinet(n);
        }
    }

    // ---------- 도우미 ----------

    private static User user(Long id, String name) {
        User u = User.of(name, name + "@example.com", null, UserRole.USER);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private Cabinet availableCabinet(int visibleNum) {
        Cabinet cabinet =
                Cabinet.of(
                        visibleNum,
                        CabinetStatus.AVAILABLE,
                        LentType.PRIVATE,
                        1,
                        null,
                        1,
                        "A",
                        1,
                        1);
        ReflectionTestUtils.setField(cabinet, "id", (long) visibleNum);
        when(cabinetRepository.findByVisibleNumWithLock(visibleNum))
                .thenReturn(Optional.of(cabinet));
        return cabinet;
    }

    private void assertHolds(Long userId, int visibleNum) {
        assertThat(reservationPort.getReservedUserId(visibleNum)).contains(userId);
        assertThat(reservationPort.getUserReservation(userId)).contains(visibleNum);
    }

    // ---------- 재예약(방안 A) ----------

    @Test
    @DisplayName("다른 사물함을 예약하면 기존 예약은 자동 취소되고 새 예약만 남는다")
    void reReserve_differentCabinet_replacesOld() {
        service.makeReservation(USER_ID, 201);
        service.makeReservation(USER_ID, 202);

        assertHolds(USER_ID, 202);
        assertThat(reservationPort.getReservedUserId(201)).isEmpty();
    }

    @Test
    @DisplayName("같은 사물함을 다시 예약하면 ALREADY_RESERVED 로 거부하고 TTL 은 그대로다")
    void reReserve_sameCabinet_isRejected() {
        service.makeReservation(USER_ID, 201);
        template.expire("cabinet:reservation:201", java.time.Duration.ofSeconds(100));
        template.expire("user:reservation:9", java.time.Duration.ofSeconds(100));

        assertThatThrownBy(() -> service.makeReservation(USER_ID, 201))
                .isInstanceOfSatisfying(
                        ServiceException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ALREADY_RESERVED));

        assertThat(reservationPort.getUserReservationTtlSeconds(USER_ID).orElseThrow())
                .isLessThanOrEqualTo(100L);
        assertHolds(USER_ID, 201);
    }

    @Test
    @DisplayName("남이 예약한 사물함은 거부하고, 내 기존 예약은 그대로 둔다")
    void reserve_takenByOther_keepsMyOldReservation() {
        service.makeReservation(USER_ID, 201);
        reservationPort.reserveReplacing(202, 77L, 15);

        assertThatThrownBy(() -> service.makeReservation(USER_ID, 202))
                .isInstanceOfSatisfying(
                        ServiceException.class,
                        e ->
                                assertThat(e.getErrorCode())
                                        .isEqualTo(ErrorCode.CABINET_ALREADY_RESERVED));

        assertHolds(USER_ID, 201);
        assertHolds(77L, 202);
    }

    @Test
    @DisplayName("새 예약이 검증에서 실패하면 기존 예약은 취소되지 않는다")
    void reserve_failedValidation_keepsOldReservation() {
        service.makeReservation(USER_ID, 201);
        Cabinet broken = availableCabinet(202);
        broken.updateStatus(CabinetStatus.BROKEN);

        assertThatThrownBy(() -> service.makeReservation(USER_ID, 202))
                .isInstanceOfSatisfying(
                        ServiceException.class,
                        e ->
                                assertThat(e.getErrorCode())
                                        .isEqualTo(ErrorCode.INVALID_CABINET_STATUS));

        assertHolds(USER_ID, 201);
        assertThat(reservationPort.getReservedUserId(202)).isEmpty();
    }

    // ---------- 레이스 ----------

    @Test
    @DisplayName("같은 유저가 서로 다른 사물함을 동시에 예약해도 예약은 항상 하나만 남는다")
    void concurrentReservations_sameUser_leavesExactlyOne() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 100; i++) {
                template.getConnectionFactory().getConnection().serverCommands().flushAll();
                CyclicBarrier barrier = new CyclicBarrier(2);
                Future<?> a = pool.submit(() -> reserveAfterBarrier(barrier, 201));
                Future<?> b = pool.submit(() -> reserveAfterBarrier(barrier, 202));
                a.get();
                b.get();

                int held =
                        (reservationPort.getReservedUserId(201).isPresent() ? 1 : 0)
                                + (reservationPort.getReservedUserId(202).isPresent() ? 1 : 0);
                assertThat(held).as("반복 %d", i).isEqualTo(1);
                Integer mine = reservationPort.getUserReservation(USER_ID).orElseThrow();
                assertThat(reservationPort.getReservedUserId(mine)).contains(USER_ID);
            }
        } finally {
            pool.shutdown();
        }
    }

    private void reserveAfterBarrier(CyclicBarrier barrier, int visibleNum) {
        try {
            barrier.await();
            service.makeReservation(USER_ID, visibleNum);
        } catch (ServiceException ignored) {
            // 동시 요청 중 하나가 "이미 예약 중" 등으로 거부되는 것은 정상이다.
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---------- 유령 선점 회귀 ----------

    @Test
    @DisplayName("101 을 예약한 뒤 102 를 대여하면 101 예약도 함께 정리된다(유령 선점 회귀)")
    void startLent_clearsReservationOnAnotherCabinet() {
        service.makeReservation(USER_ID, 101);
        ItemHistory ticket = mock(ItemHistory.class);
        when(itemHistoryRepository.findUnusedItems(USER_ID, ItemType.LENT))
                .thenReturn(List.of(ticket));

        service.startLent(USER_ID, 102);

        assertThat(reservationPort.getReservedUserId(101)).isEmpty();
        assertThat(reservationPort.getUserReservation(USER_ID)).isEmpty();
    }

    @Test
    @DisplayName("101 을 예약한 뒤 202 로 이사하면 101 예약도 함께 정리된다(유령 선점 회귀)")
    void swap_clearsReservationOnAnotherCabinet() {
        service.makeReservation(USER_ID, 101);

        Cabinet oldCabinet =
                Cabinet.of(1, CabinetStatus.FULL, LentType.PRIVATE, 1, null, 1, "A", 1, 1);
        LentHistory oldLent =
                LentHistory.of(
                        user(USER_ID, "intra09"),
                        oldCabinet,
                        LocalDateTime.now().minusDays(5),
                        LocalDateTime.now().plusDays(20));
        when(lentRepository.findByUserIdAndEndedAtIsNull(USER_ID)).thenReturn(Optional.of(oldLent));
        ItemHistory ticket = mock(ItemHistory.class);
        when(itemHistoryRepository.findUnusedItems(eq(USER_ID), eq(ItemType.SWAP)))
                .thenReturn(List.of(ticket));
        when(lentRepository.save(any(LentHistory.class))).thenAnswer(i -> i.getArgument(0));

        service.processSwapTransaction(USER_ID, 202, "1234", false, null, "photo-url");

        assertThat(reservationPort.getReservedUserId(101)).isEmpty();
        assertThat(reservationPort.getUserReservation(USER_ID)).isEmpty();
    }
}
