package com.gyeongsan.cabinet.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.storage.blob.BlobServiceClient;
import com.gyeongsan.cabinet.adapter.out.persistence.cabinet.CabinetRepository;
import com.gyeongsan.cabinet.domain.cabinet.model.Cabinet;
import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import com.gyeongsan.cabinet.domain.cabinet.model.LentType;
import com.gyeongsan.cabinet.domain.item.model.Item;
import com.gyeongsan.cabinet.domain.item.model.ItemHistory;
import com.gyeongsan.cabinet.domain.item.model.ItemType;
import com.gyeongsan.cabinet.domain.item.port.out.ItemHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.item.port.out.ItemRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.model.LentHistory;
import com.gyeongsan.cabinet.domain.lent.port.in.LentUseCase;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class LentConcurrencyIntegrationTest {

    @Container static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>("mariadb:10.6");

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MARIADB::getJdbcUrl);
        registry.add("spring.datasource.username", MARIADB::getUsername);
        registry.add("spring.datasource.password", MARIADB::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.mariadb.jdbc.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MariaDBDialect");
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    private static final AtomicInteger SEQ = new AtomicInteger(9000);

    @MockBean private BlobServiceClient blobServiceClient;

    @Autowired private LentUseCase lentUseCase;
    @Autowired private UserRepositoryPort userRepository;
    @Autowired private CabinetRepository cabinetRepository;
    @Autowired private ItemRepositoryPort itemRepository;
    @Autowired private ItemHistoryRepositoryPort itemHistoryRepository;
    @Autowired private LentRepositoryPort lentRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Value("${cabinet.policy.lent-term}")
    private int lentTerm;

    private User userWithOneLentTicket() {
        int n = SEQ.incrementAndGet();
        User user =
                userRepository.save(
                        User.of("it-user-" + n, "it" + n + "@test.com", null, UserRole.USER));
        Item lentItem = itemRepository.findByType(ItemType.LENT).orElseThrow();
        itemHistoryRepository.save(new ItemHistory(LocalDateTime.now(), null, user, lentItem));
        return user;
    }

    private Cabinet cabinet(CabinetStatus status) {
        int n = SEQ.incrementAndGet();
        return cabinetRepository.save(
                Cabinet.of(n, status, LentType.PRIVATE, 1, null, 2, "IT", 1, n));
    }

    private List<Object> runConcurrently(List<Callable<Void>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Object>> futures = new ArrayList<>();
        for (Callable<Void> task : tasks) {
            futures.add(
                    executor.submit(
                            () -> {
                                start.await();
                                try {
                                    task.call();
                                    return "OK";
                                } catch (ServiceException e) {
                                    return e.getErrorCode();
                                }
                            }));
        }
        start.countDown();
        List<Object> results = new ArrayList<>();
        for (Future<Object> future : futures) {
            results.add(future.get());
        }
        executor.shutdown();
        return results;
    }

    private int activeLentCount(Long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lent_history WHERE user_id = ? AND ended_at IS NULL",
                Integer.class,
                userId);
    }

    private int unusedLentTicketCount(Long userId) {
        return itemHistoryRepository.countByUserIdAndItemTypeAndUsedAtIsNull(userId, ItemType.LENT);
    }

    @Test
    @DisplayName("같은 유저가 사물함 두 개를 동시에 빌리면 하나만 성공한다")
    void startLent_sameUserTwoCabinets_onlyOneSucceeds() throws Exception {
        User user = userWithOneLentTicket();
        Cabinet first = cabinet(CabinetStatus.AVAILABLE);
        Cabinet second = cabinet(CabinetStatus.AVAILABLE);

        List<Object> results =
                runConcurrently(
                        List.of(
                                () -> {
                                    lentUseCase.startLent(user.getId(), first.getVisibleNum());
                                    return null;
                                },
                                () -> {
                                    lentUseCase.startLent(user.getId(), second.getVisibleNum());
                                    return null;
                                }));

        assertThat(results).containsOnlyOnce("OK");
        assertThat(results)
                .containsAnyOf(ErrorCode.LENT_ALREADY_EXIST, ErrorCode.LENT_TICKET_NOT_FOUND);
        assertThat(activeLentCount(user.getId())).isEqualTo(1);
        assertThat(unusedLentTicketCount(user.getId())).isZero();
    }

    @Test
    @DisplayName("두 유저가 같은 사물함을 동시에 빌리면 하나만 성공한다")
    void startLent_twoUsersSameCabinet_onlyOneSucceeds() throws Exception {
        User userA = userWithOneLentTicket();
        User userB = userWithOneLentTicket();
        Cabinet target = cabinet(CabinetStatus.AVAILABLE);

        List<Object> results =
                runConcurrently(
                        List.of(
                                () -> {
                                    lentUseCase.startLent(userA.getId(), target.getVisibleNum());
                                    return null;
                                },
                                () -> {
                                    lentUseCase.startLent(userB.getId(), target.getVisibleNum());
                                    return null;
                                }));

        assertThat(results).containsOnlyOnce("OK");
        assertThat(activeLentCount(userA.getId()) + activeLentCount(userB.getId())).isEqualTo(1);
        assertThat(cabinetRepository.findById(target.getId()).orElseThrow().getStatus())
                .isEqualTo(CabinetStatus.FULL);
    }

    @Test
    @DisplayName("수동 연장을 동시에 두 번 눌러도 대여권 1장으로 한 번만 연장된다")
    void manualRenew_doubleClick_extendsOnce() throws Exception {
        User user = userWithOneLentTicket();
        Cabinet cabinet = cabinet(CabinetStatus.FULL);
        LocalDateTime expiredAt = LocalDateTime.now().plusDays(3).withNano(0);
        lentRepository.save(LentHistory.of(user, cabinet, LocalDateTime.now(), expiredAt));

        List<Object> results =
                runConcurrently(
                        List.of(
                                () -> {
                                    lentUseCase.manualRenew(user.getId());
                                    return null;
                                },
                                () -> {
                                    lentUseCase.manualRenew(user.getId());
                                    return null;
                                }));

        assertThat(results).containsExactlyInAnyOrder("OK", ErrorCode.LENT_TICKET_NOT_FOUND);
        LentHistory lent = lentRepository.findByUserIdAndEndedAtIsNull(user.getId()).orElseThrow();
        assertThat(lent.getExpiredAt()).isEqualTo(expiredAt.plusDays(lentTerm));
    }
}
