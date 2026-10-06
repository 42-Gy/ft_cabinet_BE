package com.gyeongsan.cabinet.application.lent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyeongsan.cabinet.adapter.out.cache.redis.ReservationRedisAdapter;
import com.gyeongsan.cabinet.adapter.out.persistence.cabinet.CabinetRepository;
import com.gyeongsan.cabinet.adapter.out.persistence.item.ItemHistoryRepository;
import com.gyeongsan.cabinet.adapter.out.persistence.item.ItemRepository;
import com.gyeongsan.cabinet.adapter.out.persistence.lent.LentRepository;
import com.gyeongsan.cabinet.adapter.out.persistence.user.UserRepository;
import com.gyeongsan.cabinet.common.lock.DistributedLockAop;
import com.gyeongsan.cabinet.domain.cabinet.model.Cabinet;
import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import com.gyeongsan.cabinet.domain.cabinet.model.LentType;
import com.gyeongsan.cabinet.domain.cabinet.port.out.CabinetRepositoryPort;
import com.gyeongsan.cabinet.domain.item.model.Item;
import com.gyeongsan.cabinet.domain.item.model.ItemHistory;
import com.gyeongsan.cabinet.domain.item.model.ItemType;
import com.gyeongsan.cabinet.domain.item.port.out.ItemHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.model.LentHistory;
import com.gyeongsan.cabinet.domain.lent.port.in.LentUseCase;
import com.gyeongsan.cabinet.domain.lent.port.out.AiCheckPort;
import com.gyeongsan.cabinet.domain.lent.port.out.ImageUploadPort;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.ReservationPort;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import com.gyeongsan.cabinet.support.MariaDbDriverMySqlContainer;
import com.gyeongsan.cabinet.support.RedisTestSupport;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 같은 유저의 동시 요청이 대여권 한 장으로 여러 번 처리되지 않는지 실제 MySQL(운영 8.0, 테스트 8.4) + 실제 Redis 분산락 + 실제 서비스 코드로 확인한다.
 * 요청 둘을 배리어로 동시에 출발시킨다. Docker 가 없으면 건너뛴다.
 */
@Testcontainers(disabledWithoutDocker = true)
class LentConcurrencyMysqlTest {

    private static final int TRIALS = 25;

    private static GenericContainer<?> redis;
    private static LettuceConnectionFactory redisFactory;
    private static StringRedisTemplate redisTemplate;

    @BeforeAll
    static void startRedis() {
        redis = RedisTestSupport.newRedis();
        redis.start();
        redisFactory = RedisTestSupport.connectionFactory(redis);
        redisTemplate = RedisTestSupport.template(redisFactory);
    }

    @AfterAll
    static void stopRedis() {
        if (redisFactory != null) {
            redisFactory.destroy();
        }
        if (redis != null) {
            redis.stop();
        }
    }

    @Configuration
    @EnableAspectJAutoProxy
    @EnableTransactionManagement
    @EntityScan(basePackages = "com.gyeongsan.cabinet")
    @EnableJpaRepositories(basePackages = "com.gyeongsan.cabinet.adapter.out.persistence")
    @ComponentScan(
            basePackages = {
                "com.gyeongsan.cabinet.adapter.out.persistence.user",
                "com.gyeongsan.cabinet.adapter.out.persistence.cabinet",
                "com.gyeongsan.cabinet.adapter.out.persistence.lent",
                "com.gyeongsan.cabinet.adapter.out.persistence.item"
            })
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        StringRedisTemplate redisTemplate() {
            return redisTemplate;
        }

        @Bean
        DistributedLockAop distributedLockAop(StringRedisTemplate template) {
            return new DistributedLockAop(template);
        }

        @Bean
        ReservationPort reservationPort(StringRedisTemplate template) {
            return new ReservationRedisAdapter(template);
        }

        @Bean
        TransactionTemplate transactionTemplate(PlatformTransactionManager tm) {
            return new TransactionTemplate(tm);
        }

        @Bean
        AiCheckPort aiCheckPort() {
            AiCheckPort port = mock(AiCheckPort.class);
            when(port.checkItem(any())).thenReturn(true);
            return port;
        }

        @Bean
        ImageUploadPort imageUploadPort() {
            ImageUploadPort port = mock(ImageUploadPort.class);
            when(port.uploadImage(any(), any())).thenReturn("https://photo.example/test.jpg");
            return port;
        }

        @Bean
        LentApplicationService lentApplicationService(
                UserRepositoryPort users,
                CabinetRepositoryPort cabinets,
                LentRepositoryPort lents,
                ItemHistoryRepositoryPort itemHistories,
                ReservationPort reservations,
                AiCheckPort aiCheckPort,
                ImageUploadPort imageUploadPort,
                TransactionTemplate transactionTemplate) {
            return new LentApplicationService(
                    users,
                    cabinets,
                    lents,
                    itemHistories,
                    reservations,
                    aiCheckPort,
                    imageUploadPort,
                    transactionTemplate);
        }
    }

    private static void withContext(String image, Consumer<Fixture> body) throws Exception {
        try (MariaDbDriverMySqlContainer mysql = new MariaDbDriverMySqlContainer(image)) {
            mysql.start();
            redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
            Throwable[] failure = new Throwable[1];
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    DataSourceAutoConfiguration.class,
                                    DataSourceTransactionManagerAutoConfiguration.class,
                                    JdbcTemplateAutoConfiguration.class,
                                    TransactionAutoConfiguration.class,
                                    HibernateJpaAutoConfiguration.class))
                    .withUserConfiguration(TestConfig.class)
                    .withPropertyValues(
                            "spring.datasource.url=" + mysql.getJdbcUrl(),
                            "spring.datasource.username=" + mysql.getUsername(),
                            "spring.datasource.password=" + mysql.getPassword(),
                            "spring.datasource.driver-class-name=org.mariadb.jdbc.Driver",
                            "spring.jpa.hibernate.ddl-auto=create",
                            "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect",
                            "cabinet.policy.lent-term=31",
                            "cabinet.policy.extension-term=3")
                    .run(
                            context -> {
                                assertThat(context).hasNotFailed();
                                try {
                                    body.accept(new Fixture(context));
                                } catch (Throwable t) {
                                    failure[0] = t;
                                }
                            });
            if (failure[0] != null) {
                if (failure[0] instanceof Error e) {
                    throw e;
                }
                throw new RuntimeException(failure[0]);
            }
        }
    }

    /** 컨텍스트에서 꺼낸 서비스와 시드 데이터 헬퍼. */
    static final class Fixture {
        final LentUseCase service;
        final JdbcTemplate jdbc;
        final TransactionTemplate tx;
        final ReservationPort reservations;
        final UserRepository users;
        final CabinetRepository cabinets;
        final LentRepository lents;
        final ItemRepository items;
        final ItemHistoryRepository itemHistories;
        final Item lentItem;
        final Item swapItem;
        int seq = 0;

        Fixture(ApplicationContext ctx) {
            service = ctx.getBean(LentUseCase.class);
            jdbc = ctx.getBean(JdbcTemplate.class);
            tx = ctx.getBean(TransactionTemplate.class);
            reservations = ctx.getBean(ReservationPort.class);
            users = ctx.getBean(UserRepository.class);
            cabinets = ctx.getBean(CabinetRepository.class);
            lents = ctx.getBean(LentRepository.class);
            items = ctx.getBean(ItemRepository.class);
            itemHistories = ctx.getBean(ItemHistoryRepository.class);
            lentItem = tx.execute(s -> items.save(new Item("대여권", ItemType.LENT, 0L, "t")));
            swapItem = tx.execute(s -> items.save(new Item("이사권", ItemType.SWAP, 0L, "t")));
        }

        Long newUser(Item... tickets) {
            int n = ++seq;
            return tx.execute(
                    s -> {
                        User user =
                                users.save(
                                        User.of(
                                                "user" + n,
                                                "user" + n + "@example.com",
                                                UserRole.USER));
                        for (Item ticket : tickets) {
                            itemHistories.save(
                                    new ItemHistory(LocalDateTime.now(), null, user, ticket));
                        }
                        return user.getId();
                    });
        }

        int newCabinet(CabinetStatus status) {
            int visibleNum = 1000 + (++seq);
            tx.execute(
                    s ->
                            cabinets.save(
                                    Cabinet.of(
                                            visibleNum,
                                            status,
                                            LentType.PRIVATE,
                                            1,
                                            null,
                                            1,
                                            "A",
                                            1,
                                            1)));
            return visibleNum;
        }

        /** 유저가 이미 대여 중인 상태를 만든다. */
        void rent(Long userId, int visibleNum, LocalDateTime expiredAt) {
            tx.execute(
                    s -> {
                        User user = users.findById(userId).orElseThrow();
                        Cabinet cabinet = cabinets.findByVisibleNum(visibleNum).orElseThrow();
                        lents.save(
                                LentHistory.of(user, cabinet, expiredAt.minusDays(31), expiredAt));
                        return null;
                    });
        }

        int activeLents(Long userId) {
            return jdbc.queryForObject(
                    "SELECT COUNT(*) FROM lent_history WHERE user_id = ? AND ended_at IS NULL",
                    Integer.class,
                    userId);
        }

        int usedTickets(Long userId, ItemType type) {
            return jdbc.queryForObject(
                    "SELECT COUNT(*) FROM item_history ih JOIN item i ON ih.item_id = i.id"
                            + " WHERE ih.user_id = ? AND i.type = ? AND ih.used_at IS NOT NULL",
                    Integer.class,
                    userId,
                    type.name());
        }
    }

    /** 작업들을 배리어로 동시에 출발시키고, 각 결과를 "OK" 또는 ErrorCode 이름(그 외 예외는 클래스명)으로 모은다. */
    private static List<String> runConcurrently(List<Callable<Void>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        try {
            CyclicBarrier barrier = new CyclicBarrier(tasks.size());
            List<Future<String>> futures = new ArrayList<>();
            for (Callable<Void> task : tasks) {
                futures.add(
                        pool.submit(
                                () -> {
                                    barrier.await();
                                    try {
                                        task.call();
                                        return "OK";
                                    } catch (ServiceException e) {
                                        return e.getErrorCode().name();
                                    } catch (Exception e) {
                                        return e.getClass().getSimpleName();
                                    }
                                }));
            }
            List<String> results = new ArrayList<>();
            for (Future<String> f : futures) {
                results.add(f.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private static MultipartFile file() {
        return mock(MultipartFile.class);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.0", "mysql:8.4"})
    @DisplayName("같은 유저가 서로 다른 사물함 2개를 동시에 대여해도, 대여권 1장으로는 하나만 성공한다")
    void startLent_sameUserTwoCabinets(String image) throws Exception {
        withContext(
                image,
                f -> {
                    for (int n = 0; n < TRIALS; n++) {
                        Long userId = f.newUser(f.lentItem);
                        int a = f.newCabinet(CabinetStatus.AVAILABLE);
                        int b = f.newCabinet(CabinetStatus.AVAILABLE);

                        List<String> results;
                        try {
                            results =
                                    runConcurrently(
                                            List.of(
                                                    () -> {
                                                        f.service.startLent(userId, a);
                                                        return null;
                                                    },
                                                    () -> {
                                                        f.service.startLent(userId, b);
                                                        return null;
                                                    }));
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }

                        assertThat(results).as("trial %d", n).containsOnlyOnce("OK");
                        assertThat(results)
                                .as("trial %d", n)
                                .contains(ErrorCode.LENT_ALREADY_EXIST.name());
                        assertThat(f.activeLents(userId)).as("trial %d", n).isEqualTo(1);
                        assertThat(f.usedTickets(userId, ItemType.LENT)).isEqualTo(1);
                    }
                });
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.0", "mysql:8.4"})
    @DisplayName("대여권 1장으로 연장(수동 갱신)을 동시에 두 번 요청해도 한 번만 적용된다")
    void manualRenew_sameUserTwice(String image) throws Exception {
        withContext(
                image,
                f -> {
                    for (int n = 0; n < TRIALS; n++) {
                        Long userId = f.newUser(f.lentItem);
                        int cabinet = f.newCabinet(CabinetStatus.FULL);
                        LocalDateTime expiredAt = LocalDateTime.now().plusDays(10).withNano(0);
                        f.rent(userId, cabinet, expiredAt);

                        List<String> results;
                        try {
                            results =
                                    runConcurrently(
                                            List.of(
                                                    () -> {
                                                        f.service.manualRenew(userId);
                                                        return null;
                                                    },
                                                    () -> {
                                                        f.service.manualRenew(userId);
                                                        return null;
                                                    }));
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }

                        assertThat(results).as("trial %d", n).containsOnlyOnce("OK");
                        assertThat(results)
                                .as("trial %d", n)
                                .contains(ErrorCode.LENT_TICKET_NOT_FOUND.name());
                        LocalDateTime saved =
                                f.jdbc.queryForObject(
                                        "SELECT expired_at FROM lent_history"
                                                + " WHERE user_id = ? AND ended_at IS NULL",
                                        LocalDateTime.class,
                                        userId);
                        assertThat(saved).as("trial %d", n).isEqualTo(expiredAt.plusDays(31));
                    }
                });
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.0", "mysql:8.4"})
    @DisplayName("같은 유저가 서로 다른 새 사물함으로 동시에 이사해도, 이사권 1장으로는 하나만 성공하고 활성 대여는 1개다")
    void useSwap_sameUserTwoTargets(String image) throws Exception {
        withContext(
                image,
                f -> {
                    for (int n = 0; n < TRIALS; n++) {
                        Long userId = f.newUser(f.swapItem);
                        int old = f.newCabinet(CabinetStatus.FULL);
                        f.rent(userId, old, LocalDateTime.now().plusDays(10));
                        int a = f.newCabinet(CabinetStatus.AVAILABLE);
                        int b = f.newCabinet(CabinetStatus.AVAILABLE);

                        List<String> results;
                        try {
                            results =
                                    runConcurrently(
                                            List.of(
                                                    () -> {
                                                        f.service.useSwap(
                                                                userId, a, "1234", file(), false,
                                                                null);
                                                        return null;
                                                    },
                                                    () -> {
                                                        f.service.useSwap(
                                                                userId, b, "1234", file(), false,
                                                                null);
                                                        return null;
                                                    }));
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }

                        assertThat(results).as("trial %d", n).containsOnlyOnce("OK");
                        assertThat(results)
                                .as("trial %d", n)
                                .contains(ErrorCode.SWAP_TICKET_NOT_FOUND.name());
                        assertThat(f.activeLents(userId)).as("trial %d", n).isEqualTo(1);
                        assertThat(f.usedTickets(userId, ItemType.SWAP)).isEqualTo(1);
                    }
                });
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.0", "mysql:8.4"})
    @DisplayName("이사와 다른 사람의 예약이 같은 사물함에서 겹쳐도, 이사 성공과 예약 보유가 동시에 남지 않는다")
    void useSwap_vsReservationOfAnotherUser(String image) throws Exception {
        withContext(
                image,
                f -> {
                    for (int n = 0; n < TRIALS; n++) {
                        Long swapper = f.newUser(f.swapItem);
                        int old = f.newCabinet(CabinetStatus.FULL);
                        f.rent(swapper, old, LocalDateTime.now().plusDays(10));
                        Long reserver = f.newUser();
                        int target = f.newCabinet(CabinetStatus.AVAILABLE);

                        List<String> results;
                        try {
                            results =
                                    runConcurrently(
                                            List.of(
                                                    () -> {
                                                        f.service.useSwap(
                                                                swapper, target, "1234", file(),
                                                                false, null);
                                                        return null;
                                                    },
                                                    () -> {
                                                        f.service.makeReservation(reserver, target);
                                                        return null;
                                                    }));
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }

                        boolean swapped = "OK".equals(results.get(0));
                        boolean reserved =
                                f.reservations
                                        .getReservedUserId(target)
                                        .filter(reserver::equals)
                                        .isPresent();
                        assertThat(swapped && reserved)
                                .as("trial %d: 이사는 성공했는데 다른 사람의 예약이 남음 %s", n, results)
                                .isFalse();
                    }
                });
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.4"})
    @DisplayName("다른 사람이 예약한 사물함으로는 이사할 수 없다 (예약 확인이 트랜잭션 안으로 옮겨져도 동작은 같다)")
    void useSwap_rejectedWhenReservedByAnotherUser(String image) throws Exception {
        withContext(
                image,
                f -> {
                    Long swapper = f.newUser(f.swapItem);
                    int old = f.newCabinet(CabinetStatus.FULL);
                    f.rent(swapper, old, LocalDateTime.now().plusDays(10));
                    Long reserver = f.newUser();
                    int target = f.newCabinet(CabinetStatus.AVAILABLE);
                    f.service.makeReservation(reserver, target);

                    org.assertj.core.api.Assertions.assertThatThrownBy(
                                    () ->
                                            f.service.useSwap(
                                                    swapper, target, "1234", file(), false, null))
                            .isInstanceOf(ServiceException.class)
                            .extracting(e -> ((ServiceException) e).getErrorCode())
                            .isEqualTo(ErrorCode.CABINET_ALREADY_RESERVED);
                    assertThat(f.activeLents(swapper)).isEqualTo(1);
                    assertThat(f.usedTickets(swapper, ItemType.SWAP)).isZero();
                });
    }
}
