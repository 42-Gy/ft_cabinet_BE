package com.gyeongsan.cabinet.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.BulkStatusUpdateRequest;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.BulkStatusUpdateResponse;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.UndoRejection;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.UndoRequest;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.UndoResponse;
import com.gyeongsan.cabinet.adapter.out.persistence.admin.AdminActionLogPersistenceAdapter;
import com.gyeongsan.cabinet.adapter.out.persistence.cabinet.CabinetPersistenceAdapter;
import com.gyeongsan.cabinet.adapter.out.persistence.cabinet.CabinetRepository;
import com.gyeongsan.cabinet.adapter.out.persistence.lent.LentPersistenceAdapter;
import com.gyeongsan.cabinet.adapter.out.persistence.lent.LentRepository;
import com.gyeongsan.cabinet.adapter.out.persistence.user.UserPersistenceAdapter;
import com.gyeongsan.cabinet.adapter.out.persistence.user.UserRepository;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionLog;
import com.gyeongsan.cabinet.domain.admin.model.AdminActor;
import com.gyeongsan.cabinet.domain.admin.model.UndoConflictCode;
import com.gyeongsan.cabinet.domain.admin.port.out.AdminActionLogPort;
import com.gyeongsan.cabinet.domain.cabinet.model.Cabinet;
import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import com.gyeongsan.cabinet.domain.cabinet.model.LentType;
import com.gyeongsan.cabinet.domain.lent.model.LentHistory;
import com.gyeongsan.cabinet.domain.lent.model.ReservationOutcome;
import com.gyeongsan.cabinet.domain.lent.port.out.ReservationPort;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.global.exception.UndoRejectedException;
import com.gyeongsan.cabinet.support.MariaDbDriverMySqlContainer;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 사물함 일괄 변경(bulk)을 실제로 실행한 뒤 Undo 로 되돌리는 전체 흐름을 실제 MySQL 에서 확인한다. 실제 JPA 저장소와 트랜잭션, DB 가 마이크로초 아래를
 * 잘라내는 시각 저장까지 거치므로 목(mock)으로는 확인할 수 없는 부분(로그의 시각과 DB 값의 일치, 락 쿼리, 로그 JSON 왕복)을 검증한다. Docker 가 없으면
 * 건너뛴다.
 */
@Testcontainers(disabledWithoutDocker = true)
class AdminBulkUndoRoundTripMysqlTest {

    private static final AdminActor ADMIN = new AdminActor(1L, "admin01");

    /** Redis 대신 쓰는 메모리 예약 저장소. */
    static class FakeReservationPort implements ReservationPort {
        final Map<Integer, Long> reservations = new HashMap<>();

        public void reserve(Integer visibleNum, Long userId, long ttlMinutes) {
            reservations.put(visibleNum, userId);
        }

        @Override
        public Optional<Long> getReservedUserId(Integer visibleNum) {
            return Optional.ofNullable(reservations.get(visibleNum));
        }

        @Override
        public Optional<Integer> getUserReservation(Long userId) {
            return reservations.entrySet().stream()
                    .filter(e -> e.getValue().equals(userId))
                    .map(Map.Entry::getKey)
                    .findFirst();
        }

        @Override
        public ReservationOutcome reserveReplacing(
                Integer visibleNum, Long userId, long ttlMinutes) {
            reservations.put(visibleNum, userId);
            return new ReservationOutcome(ReservationOutcome.Status.RESERVED, null);
        }

        @Override
        public Optional<Long> getUserReservationTtlSeconds(Long userId) {
            return Optional.empty();
        }

        @Override
        public Optional<Integer> cancelUserReservation(Long userId) {
            Optional<Integer> mine = getUserReservation(userId);
            mine.ifPresent(reservations::remove);
            return mine;
        }

        @Override
        public void deleteReservation(Integer visibleNum, Long userId) {
            reservations.remove(visibleNum);
        }
    }

    @Configuration
    @EntityScan(basePackages = "com.gyeongsan.cabinet")
    @EnableJpaRepositories(basePackages = "com.gyeongsan.cabinet.adapter.out.persistence")
    @Import({
        CabinetPersistenceAdapter.class,
        LentPersistenceAdapter.class,
        UserPersistenceAdapter.class,
        AdminActionLogPersistenceAdapter.class,
        AdminCabinetService.class,
        AdminUndoService.class
    })
    static class Config {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        FakeReservationPort reservationPort() {
            return new FakeReservationPort();
        }
    }

    private static ApplicationContextRunner runner(MariaDbDriverMySqlContainer mysql) {
        return new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(
                                DataSourceAutoConfiguration.class,
                                DataSourceTransactionManagerAutoConfiguration.class,
                                TransactionAutoConfiguration.class,
                                HibernateJpaAutoConfiguration.class))
                .withUserConfiguration(Config.class)
                .withPropertyValues(
                        "spring.datasource.url=" + mysql.getJdbcUrl(),
                        "spring.datasource.username=" + mysql.getUsername(),
                        "spring.datasource.password=" + mysql.getPassword(),
                        "spring.datasource.driver-class-name=org.mariadb.jdbc.Driver",
                        // 이 테스트는 도메인 흐름이 대상이라 모든 테이블을 Hibernate 가 만든다.
                        // 감사 로그 테이블 정의(Flyway)와 엔티티의 일치는 별도 테스트가 검증한다.
                        "spring.jpa.hibernate.ddl-auto=create",
                        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect");
    }

    private static UndoRejection rejectionOf(Runnable action) {
        UndoRejectedException[] holder = new UndoRejectedException[1];
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(UndoRejectedException.class, e -> holder[0] = e);
        return holder[0].getRejection();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.0", "mysql:8.4"})
    @DisplayName("일괄 반납 후 Undo 하면 사물함과 대여가 원래대로 돌아오고, 충돌은 전체 거부되며, 충돌을 풀면 다시 되돌릴 수 있다")
    void bulkThenUndo(String image) {
        try (MariaDbDriverMySqlContainer mysql = new MariaDbDriverMySqlContainer(image)) {
            mysql.start();
            runner(mysql)
                    .run(
                            context -> {
                                assertThat(context).hasNotFailed();
                                UserRepository users = context.getBean(UserRepository.class);
                                CabinetRepository cabinets =
                                        context.getBean(CabinetRepository.class);
                                LentRepository lents = context.getBean(LentRepository.class);
                                AdminCabinetService bulk =
                                        context.getBean(AdminCabinetService.class);
                                AdminUndoService undo = context.getBean(AdminUndoService.class);
                                AdminActionLogPort logs = context.getBean(AdminActionLogPort.class);
                                FakeReservationPort reservations =
                                        context.getBean(FakeReservationPort.class);
                                LocalDateTime now = LocalDateTime.now();

                                // ---------- A) 왕복: 되돌리면 원래 상태와 같다 ----------
                                User u1 = users.save(user("intra01"));
                                Cabinet c1 = cabinets.save(cabinet(101, CabinetStatus.FULL));
                                Cabinet c2 = cabinets.save(cabinet(102, CabinetStatus.AVAILABLE));
                                LentHistory l1 =
                                        lents.save(
                                                LentHistory.of(
                                                        u1,
                                                        c1,
                                                        now.minusDays(20),
                                                        now.plusDays(10)));

                                BulkStatusUpdateResponse bulkResult =
                                        bulk.bulkUpdateCabinetStatus(
                                                bulkRequest(
                                                        List.of(c1.getId(), c2.getId()),
                                                        "월말 일괄 반납"),
                                                ADMIN);
                                assertThat(cabinets.findById(c1.getId()).orElseThrow().getStatus())
                                        .isEqualTo(CabinetStatus.PENDING);
                                assertThat(lents.findById(l1.getId()).orElseThrow().getEndedAt())
                                        .isNotNull();

                                UndoResponse undone =
                                        undo.undoBulkStatusUpdate(
                                                bulkResult.batchId(),
                                                new UndoRequest("실수로 일괄 반납함"),
                                                ADMIN);

                                Cabinet restored1 = cabinets.findById(c1.getId()).orElseThrow();
                                assertThat(restored1.getStatus()).isEqualTo(CabinetStatus.FULL);
                                assertThat(restored1.getStatusNote()).isEqualTo("기존 사유");
                                assertThat(cabinets.findById(c2.getId()).orElseThrow().getStatus())
                                        .isEqualTo(CabinetStatus.AVAILABLE);
                                assertThat(lents.findById(l1.getId()).orElseThrow().getEndedAt())
                                        .isNull();
                                assertThat(undone.reopenedLents()).hasSize(1);
                                assertThat(undone.restoredCabinets()).hasSize(2);

                                // Undo 자체도 감사 기록에 남고, 원본은 되돌려진 것으로 표시된다.
                                assertThat(logs.findUndoBatchIdOf(bulkResult.batchId()))
                                        .contains(undone.undoBatchId());
                                AdminActionLog undoLog =
                                        logs.findByBatchId(undone.undoBatchId()).orElseThrow();
                                assertThat(undoLog.undoOfBatchId()).isEqualTo(bulkResult.batchId());
                                assertThat(undoLog.reason()).isEqualTo("실수로 일괄 반납함");

                                // 같은 작업을 두 번 되돌릴 수 없다.
                                UndoRejection twice =
                                        rejectionOf(
                                                () ->
                                                        undo.undoBulkStatusUpdate(
                                                                bulkResult.batchId(),
                                                                new UndoRequest("다시"),
                                                                ADMIN));
                                assertThat(twice.undoneByBatchId()).isEqualTo(undone.undoBatchId());

                                // ---------- B) 유저가 그 사이 다른 사물함을 빌림 → 전체 거부 ----------
                                User u2 = users.save(user("intra02"));
                                Cabinet c3 = cabinets.save(cabinet(103, CabinetStatus.FULL));
                                Cabinet c4 = cabinets.save(cabinet(104, CabinetStatus.AVAILABLE));
                                LentHistory l2 =
                                        lents.save(
                                                LentHistory.of(
                                                        u2,
                                                        c3,
                                                        now.minusDays(20),
                                                        now.plusDays(10)));
                                BulkStatusUpdateResponse bulkB =
                                        bulk.bulkUpdateCabinetStatus(
                                                bulkRequest(List.of(c3.getId()), "월말 일괄 반납"),
                                                ADMIN);
                                Cabinet c4Full = cabinets.findById(c4.getId()).orElseThrow();
                                c4Full.updateStatus(CabinetStatus.FULL);
                                cabinets.save(c4Full);
                                lents.save(LentHistory.of(u2, c4Full, now, now.plusDays(31)));

                                UndoRejection rejectedB =
                                        rejectionOf(
                                                () ->
                                                        undo.undoBulkStatusUpdate(
                                                                bulkB.batchId(),
                                                                new UndoRequest("되돌림"),
                                                                ADMIN));
                                assertThat(rejectedB.conflicts())
                                        .extracting(UndoRejection.Conflict::code)
                                        .containsExactly(UndoConflictCode.USER_HAS_ACTIVE_LENT);
                                // 거부된 Undo 는 아무것도 바꾸지 않았다.
                                assertThat(lents.findById(l2.getId()).orElseThrow().getEndedAt())
                                        .isNotNull();
                                assertThat(cabinets.findById(c3.getId()).orElseThrow().getStatus())
                                        .isEqualTo(CabinetStatus.PENDING);
                                assertThat(logs.findUndoBatchIdOf(bulkB.batchId())).isEmpty();

                                // ---------- C) 만료된 대여 → 거부 ----------
                                User u3 = users.save(user("intra03"));
                                Cabinet c5 = cabinets.save(cabinet(105, CabinetStatus.FULL));
                                lents.save(
                                        LentHistory.of(
                                                u3, c5, now.minusDays(40), now.minusDays(1)));
                                BulkStatusUpdateResponse bulkC =
                                        bulk.bulkUpdateCabinetStatus(
                                                bulkRequest(List.of(c5.getId()), "월말 일괄 반납"),
                                                ADMIN);
                                UndoRejection rejectedC =
                                        rejectionOf(
                                                () ->
                                                        undo.undoBulkStatusUpdate(
                                                                bulkC.batchId(),
                                                                new UndoRequest("되돌림"),
                                                                ADMIN));
                                assertThat(rejectedC.conflicts())
                                        .extracting(UndoRejection.Conflict::code)
                                        .containsExactly(UndoConflictCode.LENT_EXPIRED);

                                // ---------- D) 예약이 걸리면 거부, 풀리면 다시 되돌릴 수 있다 ----------
                                User u4 = users.save(user("intra04"));
                                Cabinet c6 = cabinets.save(cabinet(106, CabinetStatus.FULL));
                                LentHistory l4 =
                                        lents.save(
                                                LentHistory.of(
                                                        u4,
                                                        c6,
                                                        now.minusDays(20),
                                                        now.plusDays(10)));
                                BulkStatusUpdateResponse bulkD =
                                        bulk.bulkUpdateCabinetStatus(
                                                bulkRequest(List.of(c6.getId()), "월말 일괄 반납"),
                                                ADMIN);
                                reservations.reserve(106, 99L, 15);
                                UndoRejection rejectedD =
                                        rejectionOf(
                                                () ->
                                                        undo.undoBulkStatusUpdate(
                                                                bulkD.batchId(),
                                                                new UndoRequest("되돌림"),
                                                                ADMIN));
                                assertThat(rejectedD.conflicts())
                                        .extracting(UndoRejection.Conflict::code)
                                        .containsExactly(UndoConflictCode.RESERVED);
                                reservations.deleteReservation(106, 99L);
                                undo.undoBulkStatusUpdate(
                                        bulkD.batchId(), new UndoRequest("재시도"), ADMIN);
                                assertThat(lents.findById(l4.getId()).orElseThrow().getEndedAt())
                                        .isNull();
                                assertThat(cabinets.findById(c6.getId()).orElseThrow().getStatus())
                                        .isEqualTo(CabinetStatus.FULL);
                            });
        }
    }

    private static User user(String name) {
        return User.of(name, name + "@example.com", null, UserRole.USER);
    }

    private static Cabinet cabinet(int visibleNum, CabinetStatus status) {
        return Cabinet.of(visibleNum, status, LentType.PRIVATE, 1, "기존 사유", 1, "A", 1, 1);
    }

    private static BulkStatusUpdateRequest bulkRequest(List<Long> ids, String reason) {
        return new BulkStatusUpdateRequest(ids, CabinetStatus.PENDING, null, null, true, reason);
    }
}
