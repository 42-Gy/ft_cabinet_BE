package com.gyeongsan.cabinet.application.lent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.CabinetStatusRequest;
import com.gyeongsan.cabinet.adapter.out.cache.redis.ReservationRedisAdapter;
import com.gyeongsan.cabinet.adapter.out.persistence.cabinet.CabinetRepository;
import com.gyeongsan.cabinet.adapter.out.persistence.item.ItemHistoryRepository;
import com.gyeongsan.cabinet.adapter.out.persistence.item.ItemRepository;
import com.gyeongsan.cabinet.adapter.out.persistence.lent.LentRepository;
import com.gyeongsan.cabinet.adapter.out.persistence.user.UserRepository;
import com.gyeongsan.cabinet.common.lock.DistributedLockAop;
import com.gyeongsan.cabinet.domain.admin.port.in.AdminCabinetUseCase;
import com.gyeongsan.cabinet.domain.admin.port.out.AdminActionLogPort;
import com.gyeongsan.cabinet.domain.admin.service.AdminCabinetService;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
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

    /**
     * 활성 대여를 읽은 직후 이만큼 기다리게 해서 "읽은 뒤 쓰기 전" 구간을 넓힌다(0 이면 꺼짐). 반납과 관리자 변경이 겹치는 경합을 우연에 맡기지 않고 재현하려는
     * 테스트용 훅이다.
     */
    private static volatile long lentReadDelayMillis = 0;

    /** AI 청결 검사 결과. false 로 두고 forceReturn=true 로 부르면 수동 반납 경로를 탄다. */
    private static final AtomicBoolean AI_OK = new AtomicBoolean(true);

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
            when(port.checkItem(any())).thenAnswer(invocation -> AI_OK.get());
            return port;
        }

        @Bean
        ImageUploadPort imageUploadPort() {
            ImageUploadPort port = mock(ImageUploadPort.class);
            when(port.uploadImage(any(), any())).thenReturn("https://photo.example/test.jpg");
            return port;
        }

        @Bean
        AdminCabinetService adminCabinetService(
                CabinetRepositoryPort cabinets, LentRepositoryPort lents) {
            return new AdminCabinetService(cabinets, lents, mock(AdminActionLogPort.class));
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
                    withReadDelay(lents),
                    itemHistories,
                    reservations,
                    aiCheckPort,
                    imageUploadPort,
                    transactionTemplate);
        }
    }

    private static LentRepositoryPort withReadDelay(LentRepositoryPort target) {
        return (LentRepositoryPort)
                Proxy.newProxyInstance(
                        LentRepositoryPort.class.getClassLoader(),
                        new Class<?>[] {LentRepositoryPort.class},
                        (proxy, method, args) -> {
                            Object result;
                            try {
                                result = method.invoke(target, args);
                            } catch (InvocationTargetException e) {
                                throw e.getCause();
                            }
                            if (lentReadDelayMillis > 0
                                    && method.getName().equals("findByUserIdAndEndedAtIsNull")) {
                                Thread.sleep(lentReadDelayMillis);
                            }
                            return result;
                        });
    }

    /**
     * image 끝에 "+index" 를 붙이면 CABINET.VISIBLE_NUM 에 유니크 인덱스를 만든 스키마로 돌린다. 엔티티에 인덱스 정의가 없어 Hibernate
     * 가 만든 스키마(기본)에서는 {@code WHERE visible_num = ? FOR UPDATE} 가 PK 순으로 테이블을 훑으며 행을 잠그지만, 인덱스가 있는
     * 스키마에서는 해당 행만 잠근다. 운영 스키마가 어느 쪽인지 알 수 없어(덤프 없음) 두 형태 모두에서 락 순서가 안전한지 확인한다.
     */
    private static void withContext(String imageSpec, Consumer<Fixture> body) throws Exception {
        boolean indexed = imageSpec.endsWith("+index");
        String image =
                indexed
                        ? imageSpec.substring(0, imageSpec.length() - "+index".length())
                        : imageSpec;
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
                                    if (indexed) {
                                        context.getBean(JdbcTemplate.class)
                                                .execute(
                                                        "CREATE UNIQUE INDEX uk_cabinet_visible_num"
                                                                + " ON cabinet (visible_num)");
                                    }
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
        final Item penaltyItem;
        final AdminCabinetUseCase admin;
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
            penaltyItem =
                    tx.execute(
                            s ->
                                    items.save(
                                            new Item(
                                                    "패널티 감면권",
                                                    ItemType.PENALTY_EXEMPTION,
                                                    0L,
                                                    "t")));
            admin = ctx.getBean(AdminCabinetUseCase.class);
        }

        void setPenalty(Long userId, int days) {
            tx.execute(
                    s -> {
                        User user = users.findById(userId).orElseThrow();
                        user.updatePenaltyDays(days);
                        users.save(user);
                        return null;
                    });
        }

        int penaltyDays(Long userId) {
            return jdbc.queryForObject(
                    "SELECT penalty_days FROM user WHERE id = ?", Integer.class, userId);
        }

        int endedLents(Long userId) {
            return jdbc.queryForObject(
                    "SELECT COUNT(*) FROM lent_history WHERE user_id = ? AND ended_at IS NOT NULL",
                    Integer.class,
                    userId);
        }

        String cabinetStatus(int visibleNum) {
            return jdbc.queryForObject(
                    "SELECT status FROM cabinet WHERE visible_num = ?", String.class, visibleNum);
        }

        String cabinetLentType(int visibleNum) {
            return jdbc.queryForObject(
                    "SELECT lent_type FROM cabinet WHERE visible_num = ?",
                    String.class,
                    visibleNum);
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

    // ---- 반납 경로와 패널티 감면 (사물함 행 락 / 사용자 행 락) ----

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.0", "mysql:8.4"})
    @DisplayName("감면권 1장으로 감면을 동시에 두 번 요청하면, 한 번만 적용되고 다른 요청은 감면권 없음 오류다(서버 오류가 아니다)")
    void usePenaltyExemption_oneTicketTwice(String image) throws Exception {
        withContext(
                image,
                f -> {
                    for (int n = 0; n < TRIALS; n++) {
                        Long userId = f.newUser(f.penaltyItem);
                        f.setPenalty(userId, 5);

                        List<String> results;
                        try {
                            results =
                                    runConcurrently(
                                            List.of(
                                                    () -> {
                                                        f.service.usePenaltyExemption(userId);
                                                        return null;
                                                    },
                                                    () -> {
                                                        f.service.usePenaltyExemption(userId);
                                                        return null;
                                                    }));
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }

                        assertThat(results).as("trial %d", n).containsOnlyOnce("OK");
                        assertThat(results)
                                .as("trial %d", n)
                                .contains(ErrorCode.PENALTY_EXEMPTION_TICKET_NOT_FOUND.name());
                        assertThat(f.penaltyDays(userId)).as("trial %d", n).isEqualTo(4);
                        assertThat(f.usedTickets(userId, ItemType.PENALTY_EXEMPTION)).isEqualTo(1);
                    }
                });
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.0", "mysql:8.4"})
    @DisplayName("감면권 2장으로 동시에 두 번 요청하면 둘 다 성공하고 패널티가 2일 줄어든다(한 요청이 실패하면 안 된다)")
    void usePenaltyExemption_twoTicketsTwice(String image) throws Exception {
        withContext(
                image,
                f -> {
                    for (int n = 0; n < TRIALS; n++) {
                        Long userId = f.newUser(f.penaltyItem, f.penaltyItem);
                        f.setPenalty(userId, 5);

                        List<String> results;
                        try {
                            results =
                                    runConcurrently(
                                            List.of(
                                                    () -> {
                                                        f.service.usePenaltyExemption(userId);
                                                        return null;
                                                    },
                                                    () -> {
                                                        f.service.usePenaltyExemption(userId);
                                                        return null;
                                                    }));
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }

                        assertThat(results).as("trial %d", n).containsExactly("OK", "OK");
                        assertThat(f.penaltyDays(userId)).as("trial %d", n).isEqualTo(3);
                        assertThat(f.usedTickets(userId, ItemType.PENALTY_EXEMPTION)).isEqualTo(2);
                    }
                });
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.4"})
    @DisplayName("패널티가 1일 남았을 때 감면권 2장으로 동시에 두 번 요청하면, 한 번만 쓰이고 감면권 한 장은 그대로 남는다")
    void usePenaltyExemption_lastPenaltyDay(String image) throws Exception {
        withContext(
                image,
                f -> {
                    for (int n = 0; n < TRIALS; n++) {
                        Long userId = f.newUser(f.penaltyItem, f.penaltyItem);
                        f.setPenalty(userId, 1);

                        List<String> results;
                        try {
                            results =
                                    runConcurrently(
                                            List.of(
                                                    () -> {
                                                        f.service.usePenaltyExemption(userId);
                                                        return null;
                                                    },
                                                    () -> {
                                                        f.service.usePenaltyExemption(userId);
                                                        return null;
                                                    }));
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }

                        assertThat(results).as("trial %d", n).containsOnlyOnce("OK");
                        assertThat(results)
                                .as("trial %d", n)
                                .contains(ErrorCode.PENALTY_NOT_FOUND.name());
                        assertThat(f.penaltyDays(userId)).as("trial %d", n).isZero();
                        assertThat(f.usedTickets(userId, ItemType.PENALTY_EXEMPTION)).isEqualTo(1);
                    }
                });
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.0", "mysql:8.4"})
    @DisplayName("같은 유저의 반납이 동시에 두 번 들어와도 한 번만 처리되고, 연체 패널티도 한 번만 붙는다")
    void return_sameUserTwice(String image) throws Exception {
        withContext(
                image,
                f -> {
                    AI_OK.set(true);
                    for (int n = 0; n < TRIALS; n++) {
                        Long userId = f.newUser();
                        int cabinet = f.newCabinet(CabinetStatus.FULL);
                        // 2일 연체: 패널티 6일
                        f.rent(userId, cabinet, LocalDateTime.now().minusDays(2));

                        List<String> results;
                        try {
                            results =
                                    runConcurrently(
                                            List.of(
                                                    () -> {
                                                        f.service.endLent(
                                                                userId, "1234", file(), false,
                                                                null);
                                                        return null;
                                                    },
                                                    () -> {
                                                        f.service.endLent(
                                                                userId, "1234", file(), false,
                                                                null);
                                                        return null;
                                                    }));
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }

                        assertThat(results).as("trial %d", n).containsOnlyOnce("OK");
                        assertThat(results)
                                .as("trial %d", n)
                                .contains(ErrorCode.LENT_NOT_FOUND.name());
                        assertThat(f.penaltyDays(userId)).as("trial %d", n).isEqualTo(6);
                        assertThat(f.endedLents(userId)).as("trial %d", n).isEqualTo(1);
                        assertThat(f.cabinetStatus(cabinet)).isEqualTo("AVAILABLE");
                    }
                });
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.0", "mysql:8.4", "mysql:8.4+index"})
    @DisplayName("반납 중에 관리자가 같은 사물함을 고장·라피신 전용으로 바꿔도 그 변경이 반납에 덮어써지지 않는다")
    void return_vsAdminStatusChange(String image) throws Exception {
        withContext(
                image,
                f -> {
                    AI_OK.set(true);
                    lentReadDelayMillis = 400;
                    try {
                        for (int n = 0; n < 5; n++) {
                            Long userId = f.newUser();
                            int cabinet = f.newCabinet(CabinetStatus.FULL);
                            f.rent(userId, cabinet, LocalDateTime.now().plusDays(10));

                            List<String> results;
                            try {
                                results =
                                        runConcurrently(
                                                List.of(
                                                        () -> {
                                                            f.service.endLent(
                                                                    userId, "1234", file(), false,
                                                                    null);
                                                            return null;
                                                        },
                                                        () -> {
                                                            // 반납이 대여를 읽은 뒤, 쓰기 전에 끼어든다.
                                                            Thread.sleep(150);
                                                            f.admin.updateCabinetStatus(
                                                                    cabinet,
                                                                    new CabinetStatusRequest(
                                                                            CabinetStatus.BROKEN,
                                                                            LentType.LAPISCINE,
                                                                            "고장"));
                                                            return null;
                                                        }));
                            } catch (Exception e) {
                                throw new IllegalStateException(e);
                            }

                            assertThat(results).as("trial %d", n).containsExactly("OK", "OK");
                            assertThat(f.endedLents(userId)).as("trial %d", n).isEqualTo(1);
                            assertThat(f.cabinetStatus(cabinet))
                                    .as("trial %d: 관리자가 정한 고장 상태가 반납에 덮어써짐", n)
                                    .isEqualTo("BROKEN");
                            assertThat(f.cabinetLentType(cabinet))
                                    .as("trial %d: 관리자가 정한 대여 유형이 반납에 덮어써짐", n)
                                    .isEqualTo("LAPISCINE");
                        }
                    } finally {
                        lentReadDelayMillis = 0;
                    }
                });
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.0", "mysql:8.4", "mysql:8.4+index"})
    @DisplayName("수동 반납(AI 검사 실패 후 강제 반납) 중에 관리자가 사물함 대여 유형을 바꿔도 그 변경이 덮어써지지 않는다")
    void manualReturn_vsAdminStatusChange(String image) throws Exception {
        withContext(
                image,
                f -> {
                    AI_OK.set(false);
                    lentReadDelayMillis = 400;
                    try {
                        for (int n = 0; n < 5; n++) {
                            Long userId = f.newUser();
                            int cabinet = f.newCabinet(CabinetStatus.FULL);
                            f.rent(userId, cabinet, LocalDateTime.now().plusDays(10));

                            List<String> results;
                            try {
                                results =
                                        runConcurrently(
                                                List.of(
                                                        () -> {
                                                            f.service.endLent(
                                                                    userId, "1234", file(), true,
                                                                    "AI 오류");
                                                            return null;
                                                        },
                                                        () -> {
                                                            Thread.sleep(150);
                                                            f.admin.updateCabinetStatus(
                                                                    cabinet,
                                                                    new CabinetStatusRequest(
                                                                            CabinetStatus.BROKEN,
                                                                            LentType.LAPISCINE,
                                                                            "고장"));
                                                            return null;
                                                        }));
                            } catch (Exception e) {
                                throw new IllegalStateException(e);
                            }

                            assertThat(results).as("trial %d", n).containsExactly("OK", "OK");
                            assertThat(f.endedLents(userId)).as("trial %d", n).isEqualTo(1);
                            // 수동 반납은 사물함을 PENDING 으로 두므로 상태는 순서에 따라 BROKEN/PENDING 둘 다 정상이다.
                            // 그러나 관리자가 바꾼 대여 유형은 어느 순서에서도 남아야 한다.
                            assertThat(f.cabinetLentType(cabinet))
                                    .as("trial %d: 관리자가 정한 대여 유형이 수동 반납에 덮어써짐", n)
                                    .isEqualTo("LAPISCINE");
                        }
                    } finally {
                        lentReadDelayMillis = 0;
                        AI_OK.set(true);
                    }
                });
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.0", "mysql:8.4", "mysql:8.4+index"})
    @DisplayName("같은 유저의 반납과 이사가 동시에 와도 데드락·서버 오류 없이 어느 한 순서대로 끝난다(대여가 바뀐 경우 반납은 새 사물함으로 다시 시도한다)")
    void return_vsSwapOfSameUser(String image) throws Exception {
        withContext(
                image,
                f -> {
                    AI_OK.set(true);
                    for (int n = 0; n < 15; n++) {
                        Long userId = f.newUser(f.swapItem);
                        int old = f.newCabinet(CabinetStatus.FULL);
                        f.rent(userId, old, LocalDateTime.now().plusDays(10));
                        int target = f.newCabinet(CabinetStatus.AVAILABLE);

                        List<String> results;
                        try {
                            results =
                                    runConcurrently(
                                            List.of(
                                                    () -> {
                                                        f.service.endLent(
                                                                userId, "1234", file(), false,
                                                                null);
                                                        return null;
                                                    },
                                                    () -> {
                                                        f.service.useSwap(
                                                                userId, target, "1234", file(),
                                                                false, null);
                                                        return null;
                                                    }));
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }

                        // 반납은 항상 성공하고, 이사는 성공하거나(그 뒤 새 사물함을 반납) 대여가 없어 실패한다.
                        assertThat(results.get(0)).as("trial %d %s", n, results).isEqualTo("OK");
                        assertThat(results.get(1))
                                .as("trial %d %s", n, results)
                                .isIn("OK", ErrorCode.LENT_NOT_FOUND.name());
                        assertThat(f.activeLents(userId)).as("trial %d %s", n, results).isZero();
                        if ("OK".equals(results.get(1))) {
                            assertThat(f.endedLents(userId)).as("trial %d", n).isEqualTo(2);
                            assertThat(f.cabinetStatus(target))
                                    .as("trial %d", n)
                                    .isEqualTo("AVAILABLE");
                        } else {
                            assertThat(f.endedLents(userId)).as("trial %d", n).isEqualTo(1);
                            assertThat(f.cabinetStatus(target))
                                    .as("trial %d", n)
                                    .isEqualTo("AVAILABLE");
                        }
                        assertThat(f.cabinetStatus(old)).as("trial %d", n).isEqualTo("AVAILABLE");
                    }
                });
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.0", "mysql:8.4", "mysql:8.4+index"})
    @DisplayName("이사 중에 관리자가 옛 사물함을 고장·라피신 전용으로 바꿔도 그 변경이 이사에 덮어써지지 않는다")
    void swap_vsAdminChangeOfOldCabinet(String image) throws Exception {
        withContext(
                image,
                f -> {
                    AI_OK.set(true);
                    lentReadDelayMillis = 400;
                    try {
                        for (int n = 0; n < 5; n++) {
                            Long userId = f.newUser(f.swapItem);
                            int old = f.newCabinet(CabinetStatus.FULL);
                            f.rent(userId, old, LocalDateTime.now().plusDays(10));
                            int target = f.newCabinet(CabinetStatus.AVAILABLE);

                            List<String> results;
                            try {
                                results =
                                        runConcurrently(
                                                List.of(
                                                        () -> {
                                                            f.service.useSwap(
                                                                    userId, target, "1234", file(),
                                                                    false, null);
                                                            return null;
                                                        },
                                                        () -> {
                                                            // 이사가 대여를 읽은 뒤, 옛 사물함을 쓰기 전에 끼어든다.
                                                            Thread.sleep(150);
                                                            f.admin.updateCabinetStatus(
                                                                    old,
                                                                    new CabinetStatusRequest(
                                                                            CabinetStatus.BROKEN,
                                                                            LentType.LAPISCINE,
                                                                            "고장"));
                                                            return null;
                                                        }));
                            } catch (Exception e) {
                                throw new IllegalStateException(e);
                            }

                            assertThat(results).as("trial %d", n).containsExactly("OK", "OK");
                            assertThat(f.activeLents(userId)).as("trial %d", n).isEqualTo(1);
                            assertThat(f.endedLents(userId)).as("trial %d", n).isEqualTo(1);
                            assertThat(f.cabinetStatus(target)).as("trial %d", n).isEqualTo("FULL");
                            assertThat(f.cabinetStatus(old))
                                    .as("trial %d: 관리자가 정한 고장 상태가 이사에 덮어써짐", n)
                                    .isEqualTo("BROKEN");
                            assertThat(f.cabinetLentType(old))
                                    .as("trial %d: 관리자가 정한 대여 유형이 이사에 덮어써짐", n)
                                    .isEqualTo("LAPISCINE");
                        }
                    } finally {
                        lentReadDelayMillis = 0;
                    }
                });
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.0", "mysql:8.4", "mysql:8.4+index"})
    @DisplayName("두 유저가 서로의 사물함으로 동시에 이사하려 해도 데드락 없이 둘 다 '사용 중' 오류로 끝난다(사물함 락은 ID 오름차순)")
    void swap_crossingSwapsDoNotDeadlock(String image) throws Exception {
        withContext(
                image,
                f -> {
                    AI_OK.set(true);
                    for (int n = 0; n < TRIALS; n++) {
                        Long a = f.newUser(f.swapItem);
                        Long b = f.newUser(f.swapItem);
                        int cabinetA = f.newCabinet(CabinetStatus.FULL);
                        int cabinetB = f.newCabinet(CabinetStatus.FULL);
                        f.rent(a, cabinetA, LocalDateTime.now().plusDays(10));
                        f.rent(b, cabinetB, LocalDateTime.now().plusDays(10));

                        List<String> results;
                        try {
                            results =
                                    runConcurrently(
                                            List.of(
                                                    () -> {
                                                        f.service.useSwap(
                                                                a, cabinetB, "1234", file(), false,
                                                                null);
                                                        return null;
                                                    },
                                                    () -> {
                                                        f.service.useSwap(
                                                                b, cabinetA, "1234", file(), false,
                                                                null);
                                                        return null;
                                                    }));
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }

                        assertThat(results)
                                .as("trial %d", n)
                                .containsExactly(
                                        ErrorCode.INVALID_CABINET_STATUS.name(),
                                        ErrorCode.INVALID_CABINET_STATUS.name());
                        assertThat(f.activeLents(a)).isEqualTo(1);
                        assertThat(f.activeLents(b)).isEqualTo(1);
                        assertThat(f.usedTickets(a, ItemType.SWAP)).isZero();
                        assertThat(f.usedTickets(b, ItemType.SWAP)).isZero();
                    }
                });
    }
}
