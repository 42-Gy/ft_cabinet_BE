package com.gyeongsan.cabinet.domain.admin.service;

import com.gyeongsan.cabinet.adapter.in.web.admin.dto.UndoRejection;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.UndoRequest;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.UndoResponse;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionLog;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionLogItem;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionTargetType;
import com.gyeongsan.cabinet.domain.admin.model.AdminActionType;
import com.gyeongsan.cabinet.domain.admin.model.AdminActor;
import com.gyeongsan.cabinet.domain.admin.model.UndoConflictCode;
import com.gyeongsan.cabinet.domain.admin.port.in.AdminUndoUseCase;
import com.gyeongsan.cabinet.domain.admin.port.out.AdminActionLogPort;
import com.gyeongsan.cabinet.domain.cabinet.model.Cabinet;
import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import com.gyeongsan.cabinet.domain.cabinet.model.LentType;
import com.gyeongsan.cabinet.domain.cabinet.port.out.CabinetRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.model.LentHistory;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.ReservationPort;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import com.gyeongsan.cabinet.global.exception.UndoRejectedException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사물함 일괄 변경(CABINET_BULK_STATUS_UPDATE)의 Undo.
 *
 * <p>원래 작업이 남긴 변경 전/후 값으로 되돌린다. 되돌리려는 대상이 원래 작업 직후 상태와 하나라도 다르면(그 사이 바뀌었으면) 아무것도 바꾸지 않고 전체를 거부하며,
 * 충돌은 한 번에 모두 알려 준다. 이 작업이 건드린 것은 사물함 상태(status, lentType, 메모)와 대여 종료(endedAt)뿐이라 코인, 아이템, 패널티는 되돌릴
 * 것이 없다.
 *
 * <p>락 순서는 사용자 → 사물함(각각 ID 오름차순)이다. 현재 startLent/이사는 사용자 행을 잠그지 않아 Undo 와 동시에 같은 사용자가 다른 사물함을 빌리면
 * 활성 대여가 둘이 될 수 있는 좁은 틈이 남아 있으며, 핵심 대여 흐름을 건드리는 변경이라 이번 범위에서 제외했다(후속 작업).
 */
@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class AdminUndoService implements AdminUndoUseCase {

    private final AdminActionLogPort adminActionLogPort;
    private final CabinetRepositoryPort cabinetRepository;
    private final LentRepositoryPort lentRepository;
    private final UserRepositoryPort userRepository;
    private final ReservationPort reservationPort;

    @Override
    public UndoResponse undoBulkStatusUpdate(
            String batchId, UndoRequest request, AdminActor actor) {
        String undoBatchId = UUID.randomUUID().toString();
        String reason = normalizedReason(request);
        log.info(
                "[UNDO:{}] 되돌리기 요청 - 원본: {}, 관리자: {}(id={}), 사유: {}",
                undoBatchId,
                batchId,
                actor.name(),
                actor.id(),
                sanitizeForLog(reason));

        if (reason == null) {
            throw new IllegalArgumentException("되돌리기 사유(reason)는 필수입니다.");
        }
        if (reason.length() > AdminActionLog.REASON_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "되돌리기 사유(reason)는 " + AdminActionLog.REASON_MAX_LENGTH + "자 이하여야 합니다.");
        }

        AdminActionLog original =
                adminActionLogPort
                        .findByBatchId(batchId)
                        .orElseThrow(
                                () -> new ServiceException(ErrorCode.ADMIN_ACTION_LOG_NOT_FOUND));
        if (original.actionType() == AdminActionType.CABINET_BULK_STATUS_UNDO) {
            throw new IllegalArgumentException("되돌리기 기록은 다시 되돌릴 수 없습니다. 필요하면 원래의 일괄 변경을 새로 실행하세요.");
        }
        if (original.actionType() != AdminActionType.CABINET_BULK_STATUS_UPDATE) {
            throw new IllegalArgumentException("지원하지 않는 작업 종류입니다: " + original.actionType());
        }

        List<AdminActionLogItem> cabinetItems = itemsOf(original, AdminActionTargetType.CABINET);
        List<AdminActionLogItem> lentItems = itemsOf(original, AdminActionTargetType.LENT_HISTORY);

        // 락: 사용자 → 사물함, 각각 ID 오름차순. 락을 먼저 잡아야 아래의 "이미 되돌렸나" 확인과 상태 비교가 확정된다.
        List<Long> userIds =
                lentItems.stream()
                        .map(i -> longOf(i.before(), "userId"))
                        .filter(Objects::nonNull)
                        .distinct()
                        .sorted()
                        .toList();
        Map<Long, User> users = new HashMap<>();
        for (Long userId : userIds) {
            userRepository.findByIdWithLock(userId).ifPresent(u -> users.put(userId, u));
        }
        List<Long> cabinetIds =
                cabinetItems.stream()
                        .map(AdminActionLogItem::targetId)
                        .distinct()
                        .sorted()
                        .toList();
        Map<Long, Cabinet> cabinets = new HashMap<>();
        for (Long cabinetId : cabinetIds) {
            cabinetRepository
                    .findByIdWithLock(cabinetId)
                    .ifPresent(c -> cabinets.put(cabinetId, c));
        }

        Optional<String> alreadyUndoneBy = adminActionLogPort.findUndoBatchIdOf(batchId);
        if (alreadyUndoneBy.isPresent()) {
            log.warn(
                    "[UNDO:{}] 거부 - 이미 되돌려진 작업: {} (Undo: {})",
                    undoBatchId,
                    batchId,
                    alreadyUndoneBy.get());
            throw new UndoRejectedException(
                    new UndoRejection(
                            alreadyUndoneBy.get(),
                            List.of(
                                    new UndoRejection.Conflict(
                                            UndoConflictCode.ALREADY_UNDONE,
                                            null,
                                            null,
                                            null,
                                            "이미 되돌려진 작업입니다: " + alreadyUndoneBy.get()))));
        }

        // DB 시각은 마이크로초까지만 저장되므로 비교와 기록 모두 마이크로초 기준으로 맞춘다.
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        List<UndoRejection.Conflict> conflicts = new ArrayList<>();

        checkCabinets(cabinetItems, cabinets, conflicts);

        Map<Long, LentHistory> lents =
                lentItems.isEmpty()
                        ? Map.of()
                        : lentRepository
                                .findAllByIds(
                                        lentItems.stream()
                                                .map(AdminActionLogItem::targetId)
                                                .toList())
                                .stream()
                                .collect(Collectors.toMap(LentHistory::getId, Function.identity()));
        checkLents(lentItems, lents, cabinets, users, userIds, now, conflicts);

        if (!conflicts.isEmpty()) {
            log.warn("[UNDO:{}] 거부 - 원본: {}, 충돌 {}건", undoBatchId, batchId, conflicts.size());
            for (UndoRejection.Conflict c : conflicts) {
                log.warn(
                        "[UNDO:{}] 충돌 - {} 사물함: {}, 사용자: {}, {}",
                        undoBatchId,
                        c.code(),
                        c.visibleNum(),
                        c.userId(),
                        sanitizeForLog(c.detail()));
            }
            throw new UndoRejectedException(new UndoRejection(null, conflicts));
        }

        // 복구: 로그의 변경 전 값을 그대로 되돌린다.
        List<AdminActionLogItem> undoItems = new ArrayList<>();
        List<UndoResponse.RestoredCabinet> restoredCabinets = new ArrayList<>();
        List<UndoResponse.ReopenedLent> reopenedLents = new ArrayList<>();

        for (AdminActionLogItem item : lentItems) {
            LentHistory lent = lents.get(item.targetId());
            lent.reopen(stringOf(item.before(), "returnMemo"));
            Cabinet cabinet = lent.getCabinet();
            reopenedLents.add(
                    new UndoResponse.ReopenedLent(
                            lent.getId(),
                            cabinet.getId(),
                            cabinet.getVisibleNum(),
                            lent.getUser().getId(),
                            lent.getUser().getName()));
            undoItems.add(inverse(item));
            log.warn(
                    "[UNDO:{}] 대여 복구 - 대여 ID: {}, 사용자: {}(id={}), 사물함: {}",
                    undoBatchId,
                    lent.getId(),
                    lent.getUser().getName(),
                    lent.getUser().getId(),
                    cabinet.getVisibleNum());
        }
        for (AdminActionLogItem item : cabinetItems) {
            Cabinet cabinet = cabinets.get(item.targetId());
            CabinetStatus fromStatus = cabinet.getStatus();
            cabinet.updateStatus(
                    statusOf(item.before()),
                    lentTypeOf(item.before()),
                    stringOf(item.before(), "statusNote"));
            restoredCabinets.add(
                    new UndoResponse.RestoredCabinet(
                            cabinet.getId(),
                            cabinet.getVisibleNum(),
                            fromStatus,
                            cabinet.getStatus()));
            undoItems.add(inverse(item));
            log.info(
                    "[UNDO:{}] 사물함 {}(id={}) 복구 - status: {} -> {}",
                    undoBatchId,
                    cabinet.getVisibleNum(),
                    cabinet.getId(),
                    fromStatus,
                    cabinet.getStatus());
        }

        Map<String, Object> requestSnapshot = new LinkedHashMap<>();
        requestSnapshot.put("undoOfBatchId", batchId);
        requestSnapshot.put("reason", reason);
        // 변경과 같은 트랜잭션에서 저장한다. 같은 원본에 대한 두 번째 Undo 는 UNIQUE 제약으로도 막힌다.
        adminActionLogPort.save(
                new AdminActionLog(
                        undoBatchId,
                        AdminActionType.CABINET_BULK_STATUS_UNDO,
                        actor,
                        reason,
                        requestSnapshot,
                        now,
                        undoItems,
                        batchId));

        log.info(
                "[UNDO:{}] 완료 - 원본: {}, 관리자: {}, 사물함 복구 {}개, 대여 복구 {}건",
                undoBatchId,
                batchId,
                actor.name(),
                restoredCabinets.size(),
                reopenedLents.size());
        return new UndoResponse(undoBatchId, batchId, restoredCabinets, reopenedLents);
    }

    private void checkCabinets(
            List<AdminActionLogItem> cabinetItems,
            Map<Long, Cabinet> cabinets,
            List<UndoRejection.Conflict> conflicts) {
        for (AdminActionLogItem item : cabinetItems) {
            Cabinet cabinet = cabinets.get(item.targetId());
            Integer visibleNum = visibleNumOf(item);
            if (cabinet == null) {
                conflicts.add(
                        new UndoRejection.Conflict(
                                UndoConflictCode.CABINET_CHANGED,
                                item.targetId(),
                                visibleNum,
                                null,
                                "사물함이 존재하지 않습니다."));
                continue;
            }
            List<String> diffs = new ArrayList<>();
            compare(diffs, "status", stringOf(item.after(), "status"), name(cabinet.getStatus()));
            compare(
                    diffs,
                    "lentType",
                    stringOf(item.after(), "lentType"),
                    name(cabinet.getLentType()));
            compare(
                    diffs,
                    "statusNote",
                    stringOf(item.after(), "statusNote"),
                    cabinet.getStatusNote());
            if (!diffs.isEmpty()) {
                conflicts.add(
                        new UndoRejection.Conflict(
                                UndoConflictCode.CABINET_CHANGED,
                                cabinet.getId(),
                                cabinet.getVisibleNum(),
                                null,
                                "원래 작업 이후 사물함이 바뀌었습니다: " + String.join(", ", diffs)));
            }
        }
    }

    private void checkLents(
            List<AdminActionLogItem> lentItems,
            Map<Long, LentHistory> lents,
            Map<Long, Cabinet> cabinets,
            Map<Long, User> users,
            List<Long> userIds,
            LocalDateTime now,
            List<UndoRejection.Conflict> conflicts) {
        if (lentItems.isEmpty()) {
            return;
        }
        List<Long> lentCabinetIds =
                lentItems.stream().map(i -> longOf(i.before(), "cabinetId")).distinct().toList();
        Map<Long, List<LentHistory>> activeByCabinet =
                lentRepository.findAllActiveLentByCabinetIds(lentCabinetIds).stream()
                        .collect(Collectors.groupingBy(l -> l.getCabinet().getId()));
        Map<Long, List<LentHistory>> activeByUser =
                lentRepository.findAllActiveLentByUserIds(userIds).stream()
                        .collect(Collectors.groupingBy(l -> l.getUser().getId()));

        for (AdminActionLogItem item : lentItems) {
            Long cabinetId = longOf(item.before(), "cabinetId");
            Long userId = longOf(item.before(), "userId");
            Cabinet cabinet = cabinets.get(cabinetId);
            Integer visibleNum = cabinet != null ? cabinet.getVisibleNum() : visibleNumOf(item);
            LentHistory lent = lents.get(item.targetId());

            if (lent == null) {
                conflicts.add(
                        new UndoRejection.Conflict(
                                UndoConflictCode.LENT_CHANGED,
                                cabinetId,
                                visibleNum,
                                userId,
                                "되살릴 대여를 찾을 수 없습니다. (대여 ID: " + item.targetId() + ")"));
                continue;
            }

            List<String> diffs = new ArrayList<>();
            LocalDateTime expectedEndedAt = microsOf(item.after(), "endedAt");
            LocalDateTime currentEndedAt =
                    lent.getEndedAt() == null
                            ? null
                            : lent.getEndedAt().truncatedTo(ChronoUnit.MICROS);
            if (!Objects.equals(expectedEndedAt, currentEndedAt)) {
                diffs.add("endedAt 기대 " + expectedEndedAt + ", 현재 " + currentEndedAt);
            }
            compare(
                    diffs,
                    "returnMemo",
                    stringOf(item.after(), "returnMemo"),
                    lent.getReturnMemo());
            if (!diffs.isEmpty()) {
                conflicts.add(
                        new UndoRejection.Conflict(
                                UndoConflictCode.LENT_CHANGED,
                                cabinetId,
                                visibleNum,
                                userId,
                                "원래 작업 이후 대여가 바뀌었습니다: " + String.join(", ", diffs)));
                continue;
            }

            if (activeByCabinet.containsKey(cabinetId)) {
                conflicts.add(
                        new UndoRejection.Conflict(
                                UndoConflictCode.CABINET_OCCUPIED,
                                cabinetId,
                                visibleNum,
                                activeByCabinet.get(cabinetId).get(0).getUser().getId(),
                                "그 사물함을 지금 다른 사용자가 대여 중입니다."));
            }
            List<LentHistory> userActive = activeByUser.get(userId);
            if (userActive != null && !userActive.isEmpty()) {
                conflicts.add(
                        new UndoRejection.Conflict(
                                UndoConflictCode.USER_HAS_ACTIVE_LENT,
                                cabinetId,
                                visibleNum,
                                userId,
                                "사용자가 지금 다른 사물함("
                                        + userActive.get(0).getCabinet().getVisibleNum()
                                        + ")을 대여 중입니다."));
            }
            if (lent.getExpiredAt().isBefore(now)) {
                conflicts.add(
                        new UndoRejection.Conflict(
                                UndoConflictCode.LENT_EXPIRED,
                                cabinetId,
                                visibleNum,
                                userId,
                                "대여 만료일(" + lent.getExpiredAt() + ")이 지나 되살리면 연체 패널티가 붙습니다."));
            }
            if (visibleNum != null && reservationPort.getReservedUserId(visibleNum).isPresent()) {
                conflicts.add(
                        new UndoRejection.Conflict(
                                UndoConflictCode.RESERVED,
                                cabinetId,
                                visibleNum,
                                userId,
                                "그 사물함에 이사권 예약이 걸려 있습니다(예약은 15분 후 만료됩니다)."));
            }
            User user = users.get(userId);
            if (user == null || user.getDeletedAt() != null) {
                conflicts.add(
                        new UndoRejection.Conflict(
                                UndoConflictCode.USER_INACTIVE,
                                cabinetId,
                                visibleNum,
                                userId,
                                "사용자가 없거나 삭제되었습니다."));
            }
        }
    }

    /** 로그 항목의 변경 전/후를 맞바꾼 항목. Undo 기록도 같은 형식이라 필요하면 다시 읽을 수 있다. */
    private static AdminActionLogItem inverse(AdminActionLogItem item) {
        return new AdminActionLogItem(
                item.targetType(),
                item.targetId(),
                item.targetLabel(),
                item.after(),
                item.before());
    }

    private static List<AdminActionLogItem> itemsOf(
            AdminActionLog log, AdminActionTargetType type) {
        return log.items().stream().filter(i -> i.targetType() == type).toList();
    }

    private static void compare(List<String> diffs, String field, String expected, String current) {
        if (!Objects.equals(expected, current)) {
            diffs.add(field + " 기대 " + expected + ", 현재 " + current);
        }
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static String stringOf(Map<String, Object> snapshot, String key) {
        Object value = snapshot == null ? null : snapshot.get(key);
        return value == null ? null : value.toString();
    }

    private static Long longOf(Map<String, Object> snapshot, String key) {
        Object value = snapshot == null ? null : snapshot.get(key);
        return value instanceof Number number ? number.longValue() : null;
    }

    private static LocalDateTime microsOf(Map<String, Object> snapshot, String key) {
        String value = stringOf(snapshot, key);
        return value == null ? null : LocalDateTime.parse(value).truncatedTo(ChronoUnit.MICROS);
    }

    private static CabinetStatus statusOf(Map<String, Object> snapshot) {
        try {
            return CabinetStatus.valueOf(stringOf(snapshot, "status"));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalStateException("감사 로그의 사물함 상태 값이 올바르지 않습니다.", e);
        }
    }

    private static LentType lentTypeOf(Map<String, Object> snapshot) {
        try {
            return LentType.valueOf(stringOf(snapshot, "lentType"));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalStateException("감사 로그의 대여 유형 값이 올바르지 않습니다.", e);
        }
    }

    private static Integer visibleNumOf(AdminActionLogItem item) {
        try {
            return item.targetLabel() == null ? null : Integer.valueOf(item.targetLabel());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String normalizedReason(UndoRequest request) {
        if (request == null || request.reason() == null || request.reason().isBlank()) {
            return null;
        }
        return request.reason().strip();
    }

    /** 관리자 입력이 로그에 줄바꿈 등 제어문자를 끼워 넣어 로그 줄을 위조하지 못하게 한다. */
    private static String sanitizeForLog(String value) {
        return value == null ? null : value.replaceAll("\\p{Cntrl}", " ");
    }
}
