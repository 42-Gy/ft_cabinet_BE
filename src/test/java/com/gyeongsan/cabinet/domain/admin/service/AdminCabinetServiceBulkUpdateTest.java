package com.gyeongsan.cabinet.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.gyeongsan.cabinet.adapter.in.web.admin.dto.BulkStatusUpdateRequest;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.BulkStatusUpdateResponse;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionLog;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionTargetType;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionType;
import com.gyeongsan.cabinet.domain.admin.model.AdminActor;
import com.gyeongsan.cabinet.domain.admin.port.out.AdminActionLogPort;
import com.gyeongsan.cabinet.domain.cabinet.model.Cabinet;
import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import com.gyeongsan.cabinet.domain.cabinet.model.LentType;
import com.gyeongsan.cabinet.domain.cabinet.port.out.CabinetRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.model.LentHistory;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.global.exception.BulkStatusUpdateRejectedException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminCabinetServiceBulkUpdateTest {

    private static final AdminActor ADMIN = new AdminActor(1L, "admin01");

    @Mock private CabinetRepositoryPort cabinetRepository;
    @Mock private LentRepositoryPort lentRepository;
    @Mock private AdminActionLogPort adminActionLogPort;

    @InjectMocks private AdminCabinetService adminCabinetService;

    private static Cabinet cabinet(long id, int visibleNum, CabinetStatus status) {
        Cabinet cabinet =
                Cabinet.of(visibleNum, status, LentType.PRIVATE, 1, "기존 사유", 1, "A", 1, 1);
        ReflectionTestUtils.setField(cabinet, "id", id);
        return cabinet;
    }

    private static LentHistory lent(long lentId, Cabinet cabinet, String userName, long userId) {
        User user = User.of(userName, userName + "@example.com", null, UserRole.USER);
        ReflectionTestUtils.setField(user, "id", userId);
        LentHistory lent =
                LentHistory.of(
                        user,
                        cabinet,
                        LocalDateTime.now().minusDays(10),
                        LocalDateTime.now().plusDays(20));
        ReflectionTestUtils.setField(lent, "id", lentId);
        return lent;
    }

    private static BulkStatusUpdateRequest request(
            List<Long> ids,
            CabinetStatus status,
            LentType lentType,
            String note,
            boolean endActiveLents) {
        return new BulkStatusUpdateRequest(ids, status, lentType, note, endActiveLents, null);
    }

    private static BulkStatusUpdateRequest requestWithReason(
            List<Long> ids, CabinetStatus status, boolean endActiveLents, String reason) {
        return new BulkStatusUpdateRequest(ids, status, null, null, endActiveLents, reason);
    }

    private void givenLocked(Cabinet... cabinets) {
        for (Cabinet cabinet : cabinets) {
            given(cabinetRepository.findByIdWithLock(cabinet.getId()))
                    .willReturn(Optional.of(cabinet));
        }
    }

    @Test
    @DisplayName("lentType 만 바꾸는 요청은 대여를 조회하지도 종료하지도 않는다")
    void lentTypeOnly_doesNotTouchLents() {
        Cabinet occupied = cabinet(1L, 101, CabinetStatus.FULL);
        givenLocked(occupied);

        BulkStatusUpdateResponse response =
                adminCabinetService.bulkUpdateCabinetStatus(
                        request(List.of(1L), null, LentType.LAPISCINE, null, false), ADMIN);

        assertThat(occupied.getStatus()).isEqualTo(CabinetStatus.FULL);
        assertThat(occupied.getLentType()).isEqualTo(LentType.LAPISCINE);
        assertThat(occupied.getStatusNote()).isEqualTo("기존 사유");
        assertThat(response.endedLents()).isEmpty();
        assertThat(response.updatedCabinets()).hasSize(1);
        verifyNoInteractions(lentRepository);
    }

    @ParameterizedTest
    @EnumSource(
            value = CabinetStatus.class,
            names = {"AVAILABLE", "BROKEN", "DISABLED", "PENDING"})
    @DisplayName("대여 중인 사물함이 있는데 endActiveLents 가 없으면 상태 종류와 무관하게 전체 거부한다")
    void occupiedWithoutFlag_rejectsWholeRequest(CabinetStatus target) {
        Cabinet occupied = cabinet(1L, 101, CabinetStatus.FULL);
        Cabinet empty = cabinet(2L, 102, CabinetStatus.AVAILABLE);
        LentHistory lent = lent(10L, occupied, "intra01", 7L);
        givenLocked(occupied, empty);
        given(lentRepository.findAllActiveLentByCabinetIds(List.of(1L, 2L)))
                .willReturn(List.of(lent));

        assertThatThrownBy(
                        () ->
                                adminCabinetService.bulkUpdateCabinetStatus(
                                        request(List.of(2L, 1L), target, null, "사유", false), ADMIN))
                .isInstanceOfSatisfying(
                        BulkStatusUpdateRejectedException.class,
                        e -> {
                            assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                            assertThat(e.getRejection().missingCabinetIds()).isEmpty();
                            assertThat(e.getRejection().occupiedCabinets())
                                    .singleElement()
                                    .satisfies(
                                            o -> {
                                                assertThat(o.cabinetId()).isEqualTo(1L);
                                                assertThat(o.visibleNum()).isEqualTo(101);
                                                assertThat(o.userId()).isEqualTo(7L);
                                                assertThat(o.userName()).isEqualTo("intra01");
                                            });
                        });

        assertThat(occupied.getStatus()).isEqualTo(CabinetStatus.FULL);
        assertThat(empty.getStatus()).isEqualTo(CabinetStatus.AVAILABLE);
        assertThat(lent.getEndedAt()).isNull();
        verifyNoInteractions(adminActionLogPort);
    }

    @ParameterizedTest
    @EnumSource(
            value = CabinetStatus.class,
            names = {"AVAILABLE", "BROKEN", "DISABLED", "PENDING"})
    @DisplayName("endActiveLents=true 면 선택된 사물함의 활성 대여만 종료하고 상태를 바꾼다")
    void occupiedWithFlag_endsOnlySelectedLents(CabinetStatus target) {
        Cabinet occupied = cabinet(1L, 101, CabinetStatus.FULL);
        Cabinet empty = cabinet(2L, 102, CabinetStatus.AVAILABLE);
        LentHistory lent = lent(10L, occupied, "intra01", 7L);
        givenLocked(occupied, empty);
        given(lentRepository.findAllActiveLentByCabinetIds(List.of(1L, 2L)))
                .willReturn(List.of(lent));

        BulkStatusUpdateResponse response =
                adminCabinetService.bulkUpdateCabinetStatus(
                        request(List.of(1L, 2L), target, null, "사유", true), ADMIN);

        assertThat(lent.getEndedAt()).isNotNull();
        assertThat(occupied.getStatus()).isEqualTo(target);
        assertThat(empty.getStatus()).isEqualTo(target);
        assertThat(response.batchId()).isNotBlank();
        assertThat(response.endedLents())
                .singleElement()
                .satisfies(
                        e -> {
                            assertThat(e.lentHistoryId()).isEqualTo(10L);
                            assertThat(e.cabinetId()).isEqualTo(1L);
                            assertThat(e.visibleNum()).isEqualTo(101);
                            assertThat(e.userId()).isEqualTo(7L);
                            assertThat(e.userName()).isEqualTo("intra01");
                        });
        assertThat(response.updatedCabinets())
                .extracting(
                        BulkStatusUpdateResponse.UpdatedCabinet::cabinetId,
                        BulkStatusUpdateResponse.UpdatedCabinet::previousStatus,
                        BulkStatusUpdateResponse.UpdatedCabinet::status)
                .containsExactly(
                        tuple(1L, CabinetStatus.FULL, target),
                        tuple(2L, CabinetStatus.AVAILABLE, target));
    }

    @Test
    @DisplayName("한 번의 일괄 반납에서 종료되는 대여는 모두 같은 종료 시각을 가진다")
    void endedLents_shareSameEndedAt() {
        Cabinet first = cabinet(1L, 101, CabinetStatus.FULL);
        Cabinet second = cabinet(2L, 102, CabinetStatus.FULL);
        LentHistory firstLent = lent(10L, first, "intra01", 7L);
        LentHistory secondLent = lent(11L, second, "intra02", 8L);
        givenLocked(first, second);
        given(lentRepository.findAllActiveLentByCabinetIds(List.of(1L, 2L)))
                .willReturn(List.of(firstLent, secondLent));

        adminCabinetService.bulkUpdateCabinetStatus(
                request(List.of(1L, 2L), CabinetStatus.AVAILABLE, null, null, true), ADMIN);

        assertThat(firstLent.getEndedAt()).isNotNull().isEqualTo(secondLent.getEndedAt());
    }

    @Test
    @DisplayName("대여자가 없는 사물함은 endActiveLents 없이도 상태를 보정할 수 있다")
    void unoccupiedCabinet_canBeCorrectedWithoutFlag() {
        Cabinet stale = cabinet(1L, 101, CabinetStatus.FULL);
        givenLocked(stale);
        given(lentRepository.findAllActiveLentByCabinetIds(List.of(1L))).willReturn(List.of());

        BulkStatusUpdateResponse response =
                adminCabinetService.bulkUpdateCabinetStatus(
                        request(List.of(1L), CabinetStatus.AVAILABLE, null, null, false), ADMIN);

        assertThat(stale.getStatus()).isEqualTo(CabinetStatus.AVAILABLE);
        assertThat(response.endedLents()).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 ID 가 섞이면 전체 거부하고 존재하는 사물함도 바꾸지 않는다")
    void missingId_rejectsWholeRequest() {
        Cabinet existing = cabinet(1L, 101, CabinetStatus.AVAILABLE);
        givenLocked(existing);
        given(cabinetRepository.findByIdWithLock(99L)).willReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                adminCabinetService.bulkUpdateCabinetStatus(
                                        request(
                                                List.of(1L, 99L),
                                                CabinetStatus.DISABLED,
                                                null,
                                                null,
                                                false),
                                        ADMIN))
                .isInstanceOfSatisfying(
                        BulkStatusUpdateRejectedException.class,
                        e -> {
                            assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                            assertThat(e.getRejection().missingCabinetIds()).containsExactly(99L);
                            assertThat(e.getMessage()).contains("99");
                        });

        assertThat(existing.getStatus()).isEqualTo(CabinetStatus.AVAILABLE);
        verifyNoInteractions(adminActionLogPort);
    }

    @Test
    @DisplayName("없는 ID 와 대여 중인 사물함이 함께 있으면 둘 다 한 번에 보고한다")
    void missingAndOccupied_reportedTogether() {
        Cabinet occupied = cabinet(1L, 101, CabinetStatus.FULL);
        LentHistory lent = lent(10L, occupied, "intra01", 7L);
        givenLocked(occupied);
        given(cabinetRepository.findByIdWithLock(99L)).willReturn(Optional.empty());
        given(lentRepository.findAllActiveLentByCabinetIds(List.of(1L))).willReturn(List.of(lent));

        assertThatThrownBy(
                        () ->
                                adminCabinetService.bulkUpdateCabinetStatus(
                                        request(
                                                List.of(1L, 99L),
                                                CabinetStatus.AVAILABLE,
                                                null,
                                                null,
                                                false),
                                        ADMIN))
                .isInstanceOfSatisfying(
                        BulkStatusUpdateRejectedException.class,
                        e -> {
                            assertThat(e.getRejection().missingCabinetIds()).containsExactly(99L);
                            assertThat(e.getRejection().occupiedCabinets()).hasSize(1);
                        });

        assertThat(lent.getEndedAt()).isNull();
    }

    @Test
    @DisplayName("사물함 행 락은 ID 오름차순으로 잡고, 락 없는 findAllById 는 쓰지 않는다")
    void locksInAscendingOrder() {
        Cabinet one = cabinet(1L, 101, CabinetStatus.AVAILABLE);
        Cabinet two = cabinet(2L, 102, CabinetStatus.AVAILABLE);
        Cabinet three = cabinet(3L, 103, CabinetStatus.AVAILABLE);
        givenLocked(one, two, three);
        given(lentRepository.findAllActiveLentByCabinetIds(anyList())).willReturn(List.of());

        adminCabinetService.bulkUpdateCabinetStatus(
                request(List.of(3L, 1L, 2L), CabinetStatus.DISABLED, null, null, false), ADMIN);

        InOrder order = inOrder(cabinetRepository);
        order.verify(cabinetRepository).findByIdWithLock(1L);
        order.verify(cabinetRepository).findByIdWithLock(2L);
        order.verify(cabinetRepository).findByIdWithLock(3L);
        verify(cabinetRepository, never()).findAllById(anyList());
    }

    @Test
    @DisplayName("중복 ID 는 한 번만 잠그고 한 번만 변경한다")
    void duplicateIds_areProcessedOnce() {
        Cabinet one = cabinet(1L, 101, CabinetStatus.AVAILABLE);
        givenLocked(one);
        given(lentRepository.findAllActiveLentByCabinetIds(List.of(1L))).willReturn(List.of());

        BulkStatusUpdateResponse response =
                adminCabinetService.bulkUpdateCabinetStatus(
                        request(List.of(1L, 1L, 1L), CabinetStatus.DISABLED, null, null, false),
                        ADMIN);

        verify(cabinetRepository, times(1)).findByIdWithLock(1L);
        assertThat(response.updatedCabinets()).hasSize(1);
    }

    @Test
    @DisplayName("사유가 있는 BROKEN 변경은 사유까지 저장된다")
    void broken_withNote_savesNote() {
        Cabinet one = cabinet(1L, 101, CabinetStatus.AVAILABLE);
        givenLocked(one);
        given(lentRepository.findAllActiveLentByCabinetIds(List.of(1L))).willReturn(List.of());

        adminCabinetService.bulkUpdateCabinetStatus(
                request(List.of(1L), CabinetStatus.BROKEN, null, "문 파손", false), ADMIN);

        assertThat(one.getStatus()).isEqualTo(CabinetStatus.BROKEN);
        assertThat(one.getStatusNote()).isEqualTo("문 파손");
    }

    @Test
    @DisplayName("사유가 비어 있으면 기존 사유를 유지한다")
    void blankNote_keepsExistingNote() {
        Cabinet one = cabinet(1L, 101, CabinetStatus.AVAILABLE);
        givenLocked(one);
        given(lentRepository.findAllActiveLentByCabinetIds(List.of(1L))).willReturn(List.of());

        adminCabinetService.bulkUpdateCabinetStatus(
                request(List.of(1L), CabinetStatus.DISABLED, null, "  ", false), ADMIN);

        assertThat(one.getStatusNote()).isEqualTo("기존 사유");
    }

    static Stream<Arguments> invalidRequests() {
        List<Long> ids = List.of(1L);
        return Stream.of(
                Arguments.of(
                        request(List.of(), CabinetStatus.AVAILABLE, null, null, false), "비어있습니다"),
                Arguments.of(request(null, CabinetStatus.AVAILABLE, null, null, false), "비어있습니다"),
                Arguments.of(
                        request(
                                Arrays.asList(1L, null),
                                CabinetStatus.AVAILABLE,
                                null,
                                null,
                                false),
                        "null"),
                Arguments.of(request(ids, CabinetStatus.FULL, null, null, false), "FULL/OVERDUE"),
                Arguments.of(
                        request(ids, CabinetStatus.OVERDUE, null, null, false), "FULL/OVERDUE"),
                Arguments.of(request(ids, CabinetStatus.BROKEN, null, null, false), "사유"),
                Arguments.of(request(ids, CabinetStatus.BROKEN, null, "   ", false), "사유"),
                Arguments.of(
                        request(ids, CabinetStatus.DISABLED, null, "가".repeat(65), false),
                        "64자 이하"),
                Arguments.of(request(ids, null, LentType.LAPISCINE, null, true), "endActiveLents"),
                Arguments.of(request(ids, CabinetStatus.FULL, null, null, true), "FULL/OVERDUE"));
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    @DisplayName("잘못된 요청은 락이나 조회에 들어가기 전에 400 대상 예외로 거부한다")
    void invalidRequest_isRejectedBeforeAnyLockOrQuery(
            BulkStatusUpdateRequest request, String messagePart) {
        assertThatThrownBy(() -> adminCabinetService.bulkUpdateCabinetStatus(request, ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(messagePart);

        verifyNoInteractions(cabinetRepository, lentRepository, adminActionLogPort);
    }

    @Test
    @DisplayName("성공하면 감사 로그를 한 번 저장하고, batchId 는 응답과 같다")
    void success_savesAuditLogOnce() {
        Cabinet occupied = cabinet(1L, 101, CabinetStatus.FULL);
        Cabinet empty = cabinet(2L, 102, CabinetStatus.AVAILABLE);
        LentHistory lent = lent(10L, occupied, "intra01", 7L);
        givenLocked(occupied, empty);
        given(lentRepository.findAllActiveLentByCabinetIds(List.of(1L, 2L)))
                .willReturn(List.of(lent));

        BulkStatusUpdateResponse response =
                adminCabinetService.bulkUpdateCabinetStatus(
                        requestWithReason(
                                List.of(2L, 1L), CabinetStatus.AVAILABLE, true, "  월말 일괄 반납  "),
                        ADMIN);

        ArgumentCaptor<AdminActionLog> captor = ArgumentCaptor.forClass(AdminActionLog.class);
        verify(adminActionLogPort, times(1)).save(captor.capture());
        AdminActionLog saved = captor.getValue();
        assertThat(saved.batchId()).isEqualTo(response.batchId());
        assertThat(saved.actionType()).isEqualTo(AdminActionType.CABINET_BULK_STATUS_UPDATE);
        assertThat(saved.actor()).isEqualTo(ADMIN);
        assertThat(saved.reason()).isEqualTo("월말 일괄 반납");
        assertThat(saved.createdAt()).isNotNull();
        assertThat(saved.request())
                .containsEntry("cabinetIds", List.of(2L, 1L))
                .containsEntry("status", "AVAILABLE")
                .containsEntry("endActiveLents", true)
                .containsEntry("reason", "월말 일괄 반납");
    }

    @Test
    @DisplayName("감사 로그에는 사물함별 변경 전/후 값과, 종료된 대여의 전/후가 모두 담긴다")
    void auditLog_containsBeforeAfterOfCabinetsAndEndedLents() {
        Cabinet occupied = cabinet(1L, 101, CabinetStatus.FULL);
        Cabinet empty = cabinet(2L, 102, CabinetStatus.AVAILABLE);
        LentHistory lent = lent(10L, occupied, "intra01", 7L);
        givenLocked(occupied, empty);
        given(lentRepository.findAllActiveLentByCabinetIds(List.of(1L, 2L)))
                .willReturn(List.of(lent));

        adminCabinetService.bulkUpdateCabinetStatus(
                request(List.of(1L, 2L), CabinetStatus.PENDING, null, "청소 필요", true), ADMIN);

        ArgumentCaptor<AdminActionLog> captor = ArgumentCaptor.forClass(AdminActionLog.class);
        verify(adminActionLogPort).save(captor.capture());
        var items = captor.getValue().items();

        var cabinetItems =
                items.stream()
                        .filter(i -> i.targetType() == AdminActionTargetType.CABINET)
                        .toList();
        assertThat(cabinetItems).hasSize(2);
        assertThat(cabinetItems.get(0).targetId()).isEqualTo(1L);
        assertThat(cabinetItems.get(0).targetLabel()).isEqualTo("101");
        assertThat(cabinetItems.get(0).before())
                .containsEntry("status", "FULL")
                .containsEntry("lentType", "PRIVATE")
                .containsEntry("statusNote", "기존 사유");
        assertThat(cabinetItems.get(0).after())
                .containsEntry("status", "PENDING")
                .containsEntry("statusNote", "청소 필요");

        var lentItems =
                items.stream()
                        .filter(i -> i.targetType() == AdminActionTargetType.LENT_HISTORY)
                        .toList();
        assertThat(lentItems).hasSize(1);
        assertThat(lentItems.get(0).targetId()).isEqualTo(10L);
        assertThat(lentItems.get(0).before())
                .containsEntry("endedAt", null)
                .containsEntry("cabinetId", 1L)
                .containsEntry("userId", 7L);
        assertThat(lentItems.get(0).after().get("endedAt")).isNotNull();
        assertThat(lentItems.get(0).after()).containsEntry("userId", 7L);
        // 사용자 이름은 로그 항목에 담지 않는다.
        assertThat(lentItems.get(0).before().toString()).doesNotContain("intra01");
    }

    @Test
    @DisplayName("lentType 만 바꾸는 요청의 로그에는 사물함 항목만 있고 대여 항목은 없다")
    void lentTypeOnly_auditLogHasNoLentItems() {
        Cabinet occupied = cabinet(1L, 101, CabinetStatus.FULL);
        givenLocked(occupied);

        adminCabinetService.bulkUpdateCabinetStatus(
                request(List.of(1L), null, LentType.LAPISCINE, null, false), ADMIN);

        ArgumentCaptor<AdminActionLog> captor = ArgumentCaptor.forClass(AdminActionLog.class);
        verify(adminActionLogPort).save(captor.capture());
        assertThat(captor.getValue().items())
                .singleElement()
                .satisfies(
                        i -> assertThat(i.targetType()).isEqualTo(AdminActionTargetType.CABINET));
        assertThat(captor.getValue().reason()).isNull();
    }

    @Test
    @DisplayName("공백뿐인 reason 은 없는 것으로 기록한다")
    void blankReason_isStoredAsNull() {
        Cabinet one = cabinet(1L, 101, CabinetStatus.AVAILABLE);
        givenLocked(one);
        given(lentRepository.findAllActiveLentByCabinetIds(List.of(1L))).willReturn(List.of());

        adminCabinetService.bulkUpdateCabinetStatus(
                requestWithReason(List.of(1L), CabinetStatus.DISABLED, false, "   "), ADMIN);

        ArgumentCaptor<AdminActionLog> captor = ArgumentCaptor.forClass(AdminActionLog.class);
        verify(adminActionLogPort).save(captor.capture());
        assertThat(captor.getValue().reason()).isNull();
    }

    @Test
    @DisplayName("작업 사유가 255자를 넘으면 락 전에 400 대상 예외로 거부한다")
    void tooLongReason_isRejectedBeforeAnyLock() {
        assertThatThrownBy(
                        () ->
                                adminCabinetService.bulkUpdateCabinetStatus(
                                        requestWithReason(
                                                List.of(1L),
                                                CabinetStatus.DISABLED,
                                                false,
                                                "가".repeat(256)),
                                        ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason");

        verifyNoInteractions(cabinetRepository, lentRepository, adminActionLogPort);
    }

    @Test
    @DisplayName("감사 로그 저장이 실패하면 예외를 삼키지 않고 던져서 트랜잭션이 롤백되게 한다")
    void auditLogFailure_propagates() {
        Cabinet one = cabinet(1L, 101, CabinetStatus.AVAILABLE);
        givenLocked(one);
        given(lentRepository.findAllActiveLentByCabinetIds(List.of(1L))).willReturn(List.of());
        org.mockito.Mockito.doThrow(new IllegalStateException("log failure"))
                .when(adminActionLogPort)
                .save(org.mockito.ArgumentMatchers.any());

        assertThatThrownBy(
                        () ->
                                adminCabinetService.bulkUpdateCabinetStatus(
                                        request(
                                                List.of(1L),
                                                CabinetStatus.DISABLED,
                                                null,
                                                null,
                                                false),
                                        ADMIN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("log failure");
    }
}
