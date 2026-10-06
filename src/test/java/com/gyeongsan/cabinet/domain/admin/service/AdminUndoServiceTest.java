package com.gyeongsan.cabinet.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.gyeongsan.cabinet.adapter.in.web.admin.dto.UndoRejection;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.UndoRequest;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.UndoResponse;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionLog;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionLogItem;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionTargetType;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionType;
import com.gyeongsan.cabinet.domain.admin.model.AdminActor;
import com.gyeongsan.cabinet.domain.admin.model.UndoConflictCode;
import com.gyeongsan.cabinet.domain.admin.port.out.AdminActionLogPort;
import com.gyeongsan.cabinet.domain.cabinet.model.Cabinet;
import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import com.gyeongsan.cabinet.domain.cabinet.model.LentType;
import com.gyeongsan.cabinet.domain.cabinet.port.out.CabinetRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.model.LentHistory;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.ReservationPort;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import com.gyeongsan.cabinet.global.exception.UndoRejectedException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminUndoServiceTest {

    private static final AdminActor ADMIN = new AdminActor(1L, "admin01");
    private static final String ORIGINAL = "orig-batch";
    private static final UndoRequest REASON = new UndoRequest("실수로 일괄 반납함");

    /** 원래 작업의 종료 시각. DB 에는 마이크로초까지만 저장되므로 로그의 나노초 값과 마이크로초 값이 같은 시각이다. */
    private static final String LOGGED_ENDED_AT = "2026-10-05T10:00:00.123456789";

    private static final LocalDateTime DB_ENDED_AT =
            LocalDateTime.parse("2026-10-05T10:00:00.123456");

    @Mock private AdminActionLogPort adminActionLogPort;
    @Mock private CabinetRepositoryPort cabinetRepository;
    @Mock private LentRepositoryPort lentRepository;
    @Mock private UserRepositoryPort userRepository;
    @Mock private ReservationPort reservationPort;
    @InjectMocks private AdminUndoService service;

    private Cabinet cabinet;
    private User user;
    private LentHistory lent;

    @BeforeEach
    void setUp() {
        // 원래 작업 직후 상태: 사물함은 AVAILABLE, 대여는 종료됨.
        cabinet = cabinet(1L, 101, CabinetStatus.AVAILABLE);
        user = user(7L, "intra01");
        lent = lent(10L, cabinet, user, DB_ENDED_AT, LocalDateTime.now().plusDays(10));

        given(cabinetRepository.findByIdWithLock(1L)).willReturn(Optional.of(cabinet));
        given(userRepository.findByIdWithLock(7L)).willReturn(Optional.of(user));
        given(lentRepository.findAllByIds(List.of(10L))).willReturn(List.of(lent));
        given(lentRepository.findAllActiveLentByCabinetIds(any())).willReturn(List.of());
        given(lentRepository.findAllActiveLentByUserIds(any())).willReturn(List.of());
        given(reservationPort.getReservedUserId(any())).willReturn(Optional.empty());
        given(adminActionLogPort.findByBatchId(ORIGINAL)).willReturn(Optional.of(original()));
        given(adminActionLogPort.findUndoBatchIdOf(ORIGINAL)).willReturn(Optional.empty());
    }

    // ---------- 도우미 ----------

    private static Map<String, Object> snap(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    private static Cabinet cabinet(long id, int visibleNum, CabinetStatus status) {
        Cabinet c = Cabinet.of(visibleNum, status, LentType.PRIVATE, 1, "기존 사유", 1, "A", 1, 1);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    private static User user(long id, String name) {
        User u = User.of(name, name + "@example.com", null, UserRole.USER);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private static LentHistory lent(
            long id, Cabinet cabinet, User user, LocalDateTime endedAt, LocalDateTime expiredAt) {
        LentHistory l = LentHistory.of(user, cabinet, LocalDateTime.now().minusDays(20), expiredAt);
        ReflectionTestUtils.setField(l, "id", id);
        ReflectionTestUtils.setField(l, "endedAt", endedAt);
        return l;
    }

    private static AdminActionLogItem cabinetItem(long id, int visibleNum) {
        return new AdminActionLogItem(
                AdminActionTargetType.CABINET,
                id,
                String.valueOf(visibleNum),
                snap("status", "FULL", "lentType", "PRIVATE", "statusNote", "기존 사유"),
                snap("status", "AVAILABLE", "lentType", "PRIVATE", "statusNote", "기존 사유"));
    }

    private static AdminActionLogItem lentItem(long lentId, long cabinetId, long userId, int vn) {
        return new AdminActionLogItem(
                AdminActionTargetType.LENT_HISTORY,
                lentId,
                String.valueOf(vn),
                snap("endedAt", null, "returnMemo", null, "cabinetId", cabinetId, "userId", userId),
                snap(
                        "endedAt",
                        LOGGED_ENDED_AT,
                        "returnMemo",
                        null,
                        "cabinetId",
                        cabinetId,
                        "userId",
                        userId));
    }

    private static AdminActionLog originalOf(AdminActionType type, List<AdminActionLogItem> items) {
        return new AdminActionLog(
                ORIGINAL,
                type,
                new AdminActor(2L, "admin02"),
                "월말 일괄 반납",
                Map.of("status", "AVAILABLE"),
                LocalDateTime.of(2026, 10, 5, 10, 0),
                items);
    }

    private static AdminActionLog original() {
        return originalOf(
                AdminActionType.CABINET_BULK_STATUS_UPDATE,
                List.of(lentItem(10L, 1L, 7L, 101), cabinetItem(1L, 101)));
    }

    private UndoRejection rejection(Runnable action) {
        UndoRejectedException[] holder = new UndoRejectedException[1];
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(UndoRejectedException.class, e -> holder[0] = e);
        return holder[0].getRejection();
    }

    private void assertNothingChanged() {
        assertThat(cabinet.getStatus()).isEqualTo(CabinetStatus.AVAILABLE);
        assertThat(lent.getEndedAt()).isNotNull();
        verify(adminActionLogPort, never()).save(any());
    }

    // ---------- 성공 ----------

    @Test
    @DisplayName("충돌이 없으면 사물함을 변경 전 상태로 복구하고 대여를 되살리며, Undo 를 감사 기록에 남긴다")
    void success_restoresCabinetReopensLentAndLogsUndo() {
        UndoResponse response = service.undoBulkStatusUpdate(ORIGINAL, REASON, ADMIN);

        assertThat(cabinet.getStatus()).isEqualTo(CabinetStatus.FULL);
        assertThat(cabinet.getLentType()).isEqualTo(LentType.PRIVATE);
        assertThat(cabinet.getStatusNote()).isEqualTo("기존 사유");
        assertThat(lent.getEndedAt()).isNull();
        assertThat(lent.getReturnMemo()).isNull();

        assertThat(response.originalBatchId()).isEqualTo(ORIGINAL);
        assertThat(response.undoBatchId()).isNotBlank().isNotEqualTo(ORIGINAL);
        assertThat(response.restoredCabinets())
                .singleElement()
                .satisfies(
                        c -> {
                            assertThat(c.visibleNum()).isEqualTo(101);
                            assertThat(c.fromStatus()).isEqualTo(CabinetStatus.AVAILABLE);
                            assertThat(c.toStatus()).isEqualTo(CabinetStatus.FULL);
                        });
        assertThat(response.reopenedLents())
                .singleElement()
                .satisfies(
                        l -> {
                            assertThat(l.lentHistoryId()).isEqualTo(10L);
                            assertThat(l.userName()).isEqualTo("intra01");
                            assertThat(l.visibleNum()).isEqualTo(101);
                        });

        ArgumentCaptor<AdminActionLog> captor = ArgumentCaptor.forClass(AdminActionLog.class);
        verify(adminActionLogPort).save(captor.capture());
        AdminActionLog saved = captor.getValue();
        assertThat(saved.batchId()).isEqualTo(response.undoBatchId());
        assertThat(saved.actionType()).isEqualTo(AdminActionType.CABINET_BULK_STATUS_UNDO);
        assertThat(saved.undoOfBatchId()).isEqualTo(ORIGINAL);
        assertThat(saved.actor()).isEqualTo(ADMIN);
        assertThat(saved.reason()).isEqualTo("실수로 일괄 반납함");
        assertThat(saved.request()).containsEntry("undoOfBatchId", ORIGINAL);
        assertThat(saved.createdAt().getNano() % 1000).isZero();
        // 항목은 원본의 변경 전/후를 맞바꾼 것이다.
        AdminActionLogItem savedCabinet =
                saved.items().stream()
                        .filter(i -> i.targetType() == AdminActionTargetType.CABINET)
                        .findFirst()
                        .orElseThrow();
        assertThat(savedCabinet.before()).containsEntry("status", "AVAILABLE");
        assertThat(savedCabinet.after()).containsEntry("status", "FULL");
        AdminActionLogItem savedLent =
                saved.items().stream()
                        .filter(i -> i.targetType() == AdminActionTargetType.LENT_HISTORY)
                        .findFirst()
                        .orElseThrow();
        assertThat(savedLent.before().get("endedAt")).isEqualTo(LOGGED_ENDED_AT);
        assertThat(savedLent.after().get("endedAt")).isNull();
    }

    @Test
    @DisplayName("로그의 나노초 시각과 DB 의 마이크로초 시각은 같은 시각으로 비교한다(충돌로 오판하지 않는다)")
    void success_nanosecondLogValueMatchesMicrosecondDbValue() {
        // setUp 의 로그는 ...123456789, DB 는 ...123456 이다.
        assertThat(service.undoBulkStatusUpdate(ORIGINAL, REASON, ADMIN)).isNotNull();
    }

    @Test
    @DisplayName("대여 항목이 없는 작업(lentType 만 바꾼 일괄 변경)은 사물함만 복구하고 대여는 조회하지 않는다")
    void success_cabinetOnlyLog_doesNotTouchLents() {
        given(adminActionLogPort.findByBatchId(ORIGINAL))
                .willReturn(
                        Optional.of(
                                originalOf(
                                        AdminActionType.CABINET_BULK_STATUS_UPDATE,
                                        List.of(cabinetItem(1L, 101)))));

        UndoResponse response = service.undoBulkStatusUpdate(ORIGINAL, REASON, ADMIN);

        assertThat(cabinet.getStatus()).isEqualTo(CabinetStatus.FULL);
        assertThat(response.reopenedLents()).isEmpty();
        verifyNoInteractions(lentRepository, userRepository, reservationPort);
    }

    @Test
    @DisplayName("락은 사용자 → 사물함 순서이고, 각각 ID 오름차순으로 잡는다")
    void locksUsersThenCabinetsInAscendingOrder() {
        Cabinet cabinet3 = cabinet(3L, 103, CabinetStatus.AVAILABLE);
        User user9 = user(9L, "intra09");
        LentHistory lent11 =
                lent(11L, cabinet3, user9, DB_ENDED_AT, LocalDateTime.now().plusDays(5));
        given(cabinetRepository.findByIdWithLock(3L)).willReturn(Optional.of(cabinet3));
        given(userRepository.findByIdWithLock(9L)).willReturn(Optional.of(user9));
        given(lentRepository.findAllByIds(any())).willReturn(List.of(lent, lent11));
        given(adminActionLogPort.findByBatchId(ORIGINAL))
                .willReturn(
                        Optional.of(
                                originalOf(
                                        AdminActionType.CABINET_BULK_STATUS_UPDATE,
                                        List.of(
                                                lentItem(11L, 3L, 9L, 103),
                                                lentItem(10L, 1L, 7L, 101),
                                                cabinetItem(3L, 103),
                                                cabinetItem(1L, 101)))));

        service.undoBulkStatusUpdate(ORIGINAL, REASON, ADMIN);

        InOrder order = inOrder(userRepository, cabinetRepository);
        order.verify(userRepository).findByIdWithLock(7L);
        order.verify(userRepository).findByIdWithLock(9L);
        order.verify(cabinetRepository).findByIdWithLock(1L);
        order.verify(cabinetRepository).findByIdWithLock(3L);
    }

    // ---------- 거부 ----------

    @Test
    @DisplayName("이미 되돌려진 작업은 ALREADY_UNDONE 으로 거부하고 어떤 Undo 였는지 알려 준다")
    void rejects_alreadyUndone() {
        given(adminActionLogPort.findUndoBatchIdOf(ORIGINAL)).willReturn(Optional.of("undo-1"));

        UndoRejection rejection =
                rejection(() -> service.undoBulkStatusUpdate(ORIGINAL, REASON, ADMIN));

        assertThat(rejection.undoneByBatchId()).isEqualTo("undo-1");
        assertThat(rejection.conflicts())
                .extracting(UndoRejection.Conflict::code)
                .containsExactly(UndoConflictCode.ALREADY_UNDONE);
        assertNothingChanged();
    }

    @Test
    @DisplayName("원래 작업 이후 사물함이 바뀌었으면 CABINET_CHANGED 로 거부한다")
    void rejects_cabinetChanged() {
        cabinet.updateStatus(CabinetStatus.FULL, LentType.PRIVATE, "기존 사유");

        UndoRejection rejection =
                rejection(() -> service.undoBulkStatusUpdate(ORIGINAL, REASON, ADMIN));

        assertThat(rejection.conflicts())
                .anySatisfy(
                        c -> {
                            assertThat(c.code()).isEqualTo(UndoConflictCode.CABINET_CHANGED);
                            assertThat(c.visibleNum()).isEqualTo(101);
                            assertThat(c.detail())
                                    .contains("status")
                                    .contains("AVAILABLE")
                                    .contains("FULL");
                        });
        verify(adminActionLogPort, never()).save(any());
        assertThat(lent.getEndedAt()).isNotNull();
    }

    @Test
    @DisplayName("사물함이 사라졌으면 CABINET_CHANGED 로 거부한다")
    void rejects_cabinetMissing() {
        given(cabinetRepository.findByIdWithLock(1L)).willReturn(Optional.empty());

        UndoRejection rejection =
                rejection(() -> service.undoBulkStatusUpdate(ORIGINAL, REASON, ADMIN));

        assertThat(rejection.conflicts())
                .extracting(UndoRejection.Conflict::code)
                .contains(UndoConflictCode.CABINET_CHANGED);
    }

    @Test
    @DisplayName("대여를 찾을 수 없으면 LENT_CHANGED 로 거부한다")
    void rejects_lentMissing() {
        given(lentRepository.findAllByIds(any())).willReturn(List.of());

        UndoRejection rejection =
                rejection(() -> service.undoBulkStatusUpdate(ORIGINAL, REASON, ADMIN));

        assertThat(rejection.conflicts())
                .extracting(UndoRejection.Conflict::code)
                .containsExactly(UndoConflictCode.LENT_CHANGED);
    }

    @Test
    @DisplayName("대여의 종료 시각이 원래 작업의 값과 다르면 LENT_CHANGED 로 거부한다")
    void rejects_lentEndedAtDiffers() {
        ReflectionTestUtils.setField(lent, "endedAt", DB_ENDED_AT.plusMinutes(5));

        UndoRejection rejection =
                rejection(() -> service.undoBulkStatusUpdate(ORIGINAL, REASON, ADMIN));

        assertThat(rejection.conflicts())
                .singleElement()
                .satisfies(
                        c -> {
                            assertThat(c.code()).isEqualTo(UndoConflictCode.LENT_CHANGED);
                            assertThat(c.detail()).contains("endedAt");
                        });
    }

    @Test
    @DisplayName("대여가 이미 다시 열려 있으면(종료 시각 없음) LENT_CHANGED 로 거부한다")
    void rejects_lentAlreadyOpen() {
        ReflectionTestUtils.setField(lent, "endedAt", null);

        UndoRejection rejection =
                rejection(() -> service.undoBulkStatusUpdate(ORIGINAL, REASON, ADMIN));

        assertThat(rejection.conflicts())
                .extracting(UndoRejection.Conflict::code)
                .containsExactly(UndoConflictCode.LENT_CHANGED);
    }

    @Test
    @DisplayName("그 사물함을 지금 다른 사용자가 빌렸으면 CABINET_OCCUPIED 로 거부한다")
    void rejects_cabinetOccupied() {
        User other = user(8L, "intra08");
        LentHistory otherLent = lent(20L, cabinet, other, null, LocalDateTime.now().plusDays(30));
        given(lentRepository.findAllActiveLentByCabinetIds(any())).willReturn(List.of(otherLent));

        UndoRejection rejection =
                rejection(() -> service.undoBulkStatusUpdate(ORIGINAL, REASON, ADMIN));

        assertThat(rejection.conflicts())
                .anySatisfy(
                        c -> {
                            assertThat(c.code()).isEqualTo(UndoConflictCode.CABINET_OCCUPIED);
                            assertThat(c.userId()).isEqualTo(8L);
                        });
        assertNothingChanged();
    }

    @Test
    @DisplayName("사용자가 그 사이 다른 사물함을 빌렸으면 USER_HAS_ACTIVE_LENT 로 거부하고 어느 사물함인지 알려 준다")
    void rejects_userHasActiveLent() {
        Cabinet otherCabinet = cabinet(2L, 202, CabinetStatus.FULL);
        LentHistory newLent = lent(21L, otherCabinet, user, null, LocalDateTime.now().plusDays(30));
        given(lentRepository.findAllActiveLentByUserIds(any())).willReturn(List.of(newLent));

        UndoRejection rejection =
                rejection(() -> service.undoBulkStatusUpdate(ORIGINAL, REASON, ADMIN));

        assertThat(rejection.conflicts())
                .singleElement()
                .satisfies(
                        c -> {
                            assertThat(c.code()).isEqualTo(UndoConflictCode.USER_HAS_ACTIVE_LENT);
                            assertThat(c.userId()).isEqualTo(7L);
                            assertThat(c.detail()).contains("202");
                        });
        assertNothingChanged();
    }

    @Test
    @DisplayName("대여 만료일이 지났으면 LENT_EXPIRED 로 거부한다")
    void rejects_lentExpired() {
        ReflectionTestUtils.setField(lent, "expiredAt", LocalDateTime.now().minusHours(1));

        UndoRejection rejection =
                rejection(() -> service.undoBulkStatusUpdate(ORIGINAL, REASON, ADMIN));

        assertThat(rejection.conflicts())
                .extracting(UndoRejection.Conflict::code)
                .containsExactly(UndoConflictCode.LENT_EXPIRED);
        assertNothingChanged();
    }

    @Test
    @DisplayName("사물함에 예약이 걸려 있으면 RESERVED 로 거부한다")
    void rejects_reserved() {
        given(reservationPort.getReservedUserId(101)).willReturn(Optional.of(99L));

        UndoRejection rejection =
                rejection(() -> service.undoBulkStatusUpdate(ORIGINAL, REASON, ADMIN));

        assertThat(rejection.conflicts())
                .extracting(UndoRejection.Conflict::code)
                .containsExactly(UndoConflictCode.RESERVED);
        assertNothingChanged();
    }

    @Test
    @DisplayName("사용자가 삭제되었으면 USER_INACTIVE 로 거부한다")
    void rejects_userDeleted() {
        ReflectionTestUtils.setField(user, "deletedAt", LocalDateTime.now().minusDays(1));

        UndoRejection rejection =
                rejection(() -> service.undoBulkStatusUpdate(ORIGINAL, REASON, ADMIN));

        assertThat(rejection.conflicts())
                .extracting(UndoRejection.Conflict::code)
                .containsExactly(UndoConflictCode.USER_INACTIVE);
    }

    @Test
    @DisplayName("충돌이 여러 개면 한 번에 모두 알려 주고, 아무것도 바꾸지 않는다")
    void rejects_allConflictsAtOnce() {
        cabinet.updateStatus(CabinetStatus.FULL, LentType.PRIVATE, "기존 사유");
        User other = user(8L, "intra08");
        given(lentRepository.findAllActiveLentByCabinetIds(any()))
                .willReturn(
                        List.of(lent(20L, cabinet, other, null, LocalDateTime.now().plusDays(30))));
        ReflectionTestUtils.setField(lent, "expiredAt", LocalDateTime.now().minusDays(1));

        UndoRejection rejection =
                rejection(() -> service.undoBulkStatusUpdate(ORIGINAL, REASON, ADMIN));

        assertThat(rejection.conflicts())
                .extracting(UndoRejection.Conflict::code)
                .containsExactlyInAnyOrder(
                        UndoConflictCode.CABINET_CHANGED,
                        UndoConflictCode.CABINET_OCCUPIED,
                        UndoConflictCode.LENT_EXPIRED);
        verify(adminActionLogPort, never()).save(any());
        assertThat(lent.getEndedAt()).isNotNull();
    }

    // ---------- 입력 검증 ----------

    @Test
    @DisplayName("사유가 없거나 공백이면 조회·락 전에 400 대상 예외로 거부한다")
    void rejects_missingReason() {
        for (UndoRequest request :
                new UndoRequest[] {null, new UndoRequest(null), new UndoRequest("   ")}) {
            assertThatThrownBy(() -> service.undoBulkStatusUpdate(ORIGINAL, request, ADMIN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reason");
        }
        verifyNoInteractions(adminActionLogPort, cabinetRepository, lentRepository, userRepository);
    }

    @Test
    @DisplayName("사유가 255자를 넘으면 거부한다")
    void rejects_tooLongReason() {
        assertThatThrownBy(
                        () ->
                                service.undoBulkStatusUpdate(
                                        ORIGINAL, new UndoRequest("가".repeat(256)), ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("255");
        verifyNoInteractions(adminActionLogPort, cabinetRepository);
    }

    @Test
    @DisplayName("없는 batchId 는 404 로 처리한다")
    void rejects_unknownBatch() {
        given(adminActionLogPort.findByBatchId("none")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.undoBulkStatusUpdate("none", REASON, ADMIN))
                .isInstanceOfSatisfying(
                        ServiceException.class,
                        e ->
                                assertThat(e.getErrorCode())
                                        .isEqualTo(ErrorCode.ADMIN_ACTION_LOG_NOT_FOUND));
        verifyNoInteractions(cabinetRepository, lentRepository);
    }

    @Test
    @DisplayName("Undo 기록은 다시 되돌릴 수 없다")
    void rejects_undoOfUndo() {
        given(adminActionLogPort.findByBatchId("undo-1"))
                .willReturn(
                        Optional.of(
                                new AdminActionLog(
                                        "undo-1",
                                        AdminActionType.CABINET_BULK_STATUS_UNDO,
                                        ADMIN,
                                        "되돌림",
                                        Map.of(),
                                        LocalDateTime.now(),
                                        List.of(),
                                        ORIGINAL)));

        assertThatThrownBy(() -> service.undoBulkStatusUpdate("undo-1", REASON, ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("되돌릴 수 없습니다");
        verifyNoInteractions(cabinetRepository, lentRepository);
    }
}
