package com.gyeongsan.cabinet.application.lent;

import com.gyeongsan.cabinet.common.lock.DistributedLock;
import com.gyeongsan.cabinet.domain.cabinet.model.Cabinet;
import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import com.gyeongsan.cabinet.domain.cabinet.model.LentType;
import com.gyeongsan.cabinet.domain.cabinet.port.out.CabinetRepositoryPort;
import com.gyeongsan.cabinet.domain.item.model.ItemHistory;
import com.gyeongsan.cabinet.domain.item.model.ItemType;
import com.gyeongsan.cabinet.domain.item.port.out.ItemHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.model.LentHistory;
import com.gyeongsan.cabinet.domain.lent.model.LentReturnResult;
import com.gyeongsan.cabinet.domain.lent.model.ReservationOutcome;
import com.gyeongsan.cabinet.domain.lent.port.in.LentUseCase;
import com.gyeongsan.cabinet.domain.lent.port.out.AiCheckPort;
import com.gyeongsan.cabinet.domain.lent.port.out.ImageUploadPort;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.ReservationPort;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Log4j2
public class LentApplicationService implements LentUseCase {

    private final UserRepositoryPort userRepository;
    private final CabinetRepositoryPort cabinetRepository;
    private final LentRepositoryPort lentRepository;
    private final ItemHistoryRepositoryPort itemHistoryRepository;
    private final ReservationPort reservationPort;
    private final AiCheckPort aiCheckPort;
    private final ImageUploadPort imageUploadPort;
    private final TransactionTemplate transactionTemplate;

    private static final long RESERVATION_TTL_MINUTES = 15;

    /** 반납 중 대여가 다른 사물함으로 바뀌는 경우의 재시도 한도(정상 흐름에서는 1번이면 끝난다). */
    private static final int MAX_RETURN_LOCK_ATTEMPTS = 3;

    @Value("${cabinet.policy.lent-term}")
    private int lentTerm;

    @Value("${cabinet.policy.extension-term}")
    private long extensionTerm;

    @Override
    @Transactional
    @DistributedLock(key = "cabinet_lent", identifier = "#visibleNum")
    public void startLent(Long userId, Integer visibleNum) {
        log.info("대여 시도 - User: {}, Cabinet Num: {}", userId, visibleNum);

        checkCabinetReservation(visibleNum, userId);

        // 같은 유저의 동시 요청(다른 사물함 두 개 등)을 직렬화한다. 락 순서는 항상 사용자 -> 사물함이다.
        User user = lockUser(userId);

        if (user.getPenaltyDays() > 0) {
            throw new ServiceException(ErrorCode.PENALTY_USER);
        }

        Cabinet cabinet =
                cabinetRepository
                        .findByVisibleNumWithLock(visibleNum)
                        .orElseThrow(() -> new ServiceException(ErrorCode.CABINET_NOT_FOUND));

        if (lentRepository.findByUserIdAndEndedAtIsNull(userId).isPresent()) {
            throw new ServiceException(ErrorCode.LENT_ALREADY_EXIST);
        }

        if (cabinet.getStatus() != CabinetStatus.AVAILABLE) {
            throw new ServiceException(ErrorCode.INVALID_CABINET_STATUS);
        }

        validateLentTypePermission(user, cabinet);

        List<ItemHistory> lentTickets =
                itemHistoryRepository.findUnusedItems(userId, ItemType.LENT);

        if (lentTickets.isEmpty()) {
            throw new ServiceException(ErrorCode.LENT_TICKET_NOT_FOUND);
        }

        ItemHistory ticket = lentTickets.get(0);
        ticket.use();

        cabinet.updateStatus(CabinetStatus.FULL);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = now.plusDays(lentTerm);

        LentHistory lentHistory = LentHistory.of(user, cabinet, now, expiredAt);
        lentRepository.save(lentHistory);

        reservationPort.deleteReservation(visibleNum, userId);

        log.info("대여 성공! 대여 ID: {}", lentHistory.getId());
    }

    @Override
    public void checkLentCabinetImage(Long userId, MultipartFile file) {
        log.info("AI 반납 전 선검증 시도 - User: {}", userId);
        boolean isClean = aiCheckPort.checkItem(file);
        if (!isClean) {
            throw new ServiceException(ErrorCode.CABINET_NOT_EMPTY);
        }
        log.info("AI 선검증 성공 - User: {}", userId);
    }

    @Override
    public LentReturnResult endLent(
            Long userId,
            String previousPassword,
            MultipartFile file,
            Boolean forceReturn,
            String reason) {
        // 사유는 사용자가 직접 쓴 글이라 로그에 남기지 않는다(작성 여부만 남긴다).
        log.info(
                "AI 반납 시도 - User: {}, Force: {}, 사유 작성: {}",
                userId,
                forceReturn,
                reason != null && !reason.isBlank());

        boolean isAiSuccess = false;
        try {
            isAiSuccess = aiCheckPort.checkItem(file);
        } catch (ServiceException e) {
            if (!e.getErrorCode().equals(ErrorCode.CABINET_NOT_EMPTY)
                    && !(e.getErrorCode().equals(ErrorCode.INVALID_IMAGE) && forceReturn)) {
                throw e;
            }
        }

        if (!isAiSuccess && !forceReturn) {
            throw new ServiceException(ErrorCode.CABINET_NOT_EMPTY);
        }

        boolean doManualReturn = !isAiSuccess && forceReturn;
        String photoUrl = imageUploadPort.uploadImage(userId, file);

        return returnWithLocks(
                userId,
                () -> {
                    if (doManualReturn) {
                        String returnReason =
                                (reason != null && !reason.isBlank())
                                        ? "[User Force] " + reason
                                        : "AI 검사 실패 및 강제 반납";
                        return endLentManual(userId, previousPassword, returnReason, photoUrl);
                    }
                    return processReturnTransaction(userId, previousPassword, photoUrl);
                });
    }

    /**
     * 반납 트랜잭션을 사용자 행 락 -> 사물함 행 락 순서로 잠근 뒤 실행한다. 사용자 락은 같은 유저의 동시 반납·연장·이사를 직렬화하고, 사물함 락은 관리자의 상태
     * 변경( {@code findByIdWithLock})·Undo 와 겹쳐 사물함 행을 서로 덮어쓰는 것을 막는다. 락 순서는 대여·이사·Undo 와 같은 사용자 ->
     * 사물함이다(반대로 잡는 경로가 하나라도 있으면 데드락이 난다).
     *
     * <p>사물함 ID 는 락 전에 알아야 하는데 대여 행을 락 전에 읽으면 스냅샷이 잡혀 락 뒤에도 옛 값이 보이므로, ID 만 트랜잭션 밖에서 가볍게 읽는다. 그 사이
     * 이사 등으로 대여가 다른 사물함으로 바뀌었으면(락을 잡은 뒤 확인) 처음부터 다시 한다.
     */
    private LentReturnResult returnWithLocks(Long userId, Supplier<LentReturnResult> body) {
        for (int attempt = 0; attempt < MAX_RETURN_LOCK_ATTEMPTS; attempt++) {
            Long cabinetId =
                    lentRepository
                            .findActiveCabinetIdByUserId(userId)
                            .orElseThrow(() -> new ServiceException(ErrorCode.LENT_NOT_FOUND));
            try {
                return transactionTemplate.execute(
                        status -> {
                            // 이 트랜잭션의 첫 DB 조회여야 한다(REPEATABLE READ 스냅샷 때문).
                            lockUser(userId);
                            cabinetRepository
                                    .findByIdWithLock(cabinetId)
                                    .orElseThrow(
                                            () ->
                                                    new ServiceException(
                                                            ErrorCode.CABINET_NOT_FOUND));

                            Long lockedCabinetId =
                                    lentRepository
                                            .findActiveCabinetIdByUserId(userId)
                                            .orElseThrow(
                                                    () ->
                                                            new ServiceException(
                                                                    ErrorCode.LENT_NOT_FOUND));
                            if (!lockedCabinetId.equals(cabinetId)) {
                                throw new ReturnTargetChangedException();
                            }
                            return body.get();
                        });
            } catch (ReturnTargetChangedException e) {
                log.info("반납 대상 사물함이 바뀌어 다시 시도합니다 - User: {}", userId);
            }
        }
        throw new ServiceException(ErrorCode.REQUEST_IN_PROGRESS);
    }

    /** 락을 잡는 사이 사용자의 대여가 다른 사물함으로 바뀐 경우. 반납을 처음부터 다시 시도하는 신호로만 쓴다. */
    private static final class ReturnTargetChangedException extends RuntimeException {
        ReturnTargetChangedException() {
            super(null, null, false, false);
        }
    }

    public LentReturnResult endLentManual(
            Long userId, String previousPassword, String reason, String photoUrl) {
        LentHistory lentHistory =
                lentRepository
                        .findByUserIdAndEndedAtIsNull(userId)
                        .orElseThrow(() -> new ServiceException(ErrorCode.LENT_NOT_FOUND));

        User user = lentHistory.getUser();
        LocalDateTime now = LocalDateTime.now();
        LentReturnResult result =
                buildReturnResult(lentHistory, checkAndApplyPenalty(user, lentHistory, now), now);

        lentHistory.endLent(now, previousPassword);
        lentHistory.attachReturnPhoto(photoUrl);

        Cabinet cabinet = lentHistory.getCabinet();
        cabinet.updateStatus(CabinetStatus.PENDING);
        cabinet.updateStatusNote(reason);
        return result;
    }

    protected LentReturnResult processReturnTransaction(
            Long userId, String previousPassword, String photoUrl) {
        LentHistory lentHistory =
                lentRepository
                        .findByUserIdAndEndedAtIsNull(userId)
                        .orElseThrow(() -> new ServiceException(ErrorCode.LENT_NOT_FOUND));

        User user = lentHistory.getUser();
        LocalDateTime now = LocalDateTime.now();
        LentReturnResult result =
                buildReturnResult(lentHistory, checkAndApplyPenalty(user, lentHistory, now), now);

        Cabinet cabinet = lentHistory.getCabinet();

        lentHistory.endLent(now, previousPassword);
        lentHistory.attachReturnPhoto(photoUrl);

        if (cabinet.getStatus() == CabinetStatus.FULL
                || cabinet.getStatus() == CabinetStatus.OVERDUE) {
            cabinet.updateStatus(CabinetStatus.AVAILABLE);
        }

        log.info("반납 성공! 대여 ID: {}, 사물함: {}", lentHistory.getId(), cabinet.getVisibleNum());
        return result;
    }

    @Override
    @Transactional
    public void useExtension(Long userId) {
        log.info("연장권 사용 시도 - User: {}", userId);

        userRepository
                .findByIdWithLock(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        LentHistory lentHistory =
                lentRepository
                        .findByUserIdAndEndedAtIsNull(userId)
                        .orElseThrow(() -> new ServiceException(ErrorCode.LENT_NOT_FOUND));

        List<ItemHistory> extensionTickets =
                itemHistoryRepository.findUnusedItems(userId, ItemType.EXTENSION);

        if (extensionTickets.isEmpty()) {
            throw new ServiceException(ErrorCode.EXTENSION_TICKET_NOT_FOUND);
        }

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));
        if (user.isPisciner()) {
            throw new ServiceException(ErrorCode.PISCINER_EXTENSION_RESTRICTED);
        }

        ItemHistory ticket = extensionTickets.get(0);
        ticket.use();

        lentHistory.extendExpiration(extensionTerm);

        log.info("연장 성공! 변경된 만료일: {}", lentHistory.getExpiredAt());
    }

    @Override
    @Transactional
    public void manualRenew(Long userId) {
        log.info("수동 연장(대여권 사용) 시도 - User: {}", userId);

        // 같은 대여권으로 연장이 두 번 적용되지 않도록 같은 유저의 동시 요청을 직렬화한다.
        User user = lockUser(userId);

        if (user.getPenaltyDays() > 0) {
            throw new ServiceException(ErrorCode.PENALTY_USER);
        }

        if (user.isPisciner()) {
            throw new ServiceException(ErrorCode.PISCINER_EXTENSION_RESTRICTED);
        }

        LentHistory lentHistory =
                lentRepository
                        .findByUserIdAndEndedAtIsNull(userId)
                        .orElseThrow(() -> new ServiceException(ErrorCode.LENT_NOT_FOUND));

        List<ItemHistory> lentTickets =
                itemHistoryRepository.findUnusedItems(userId, ItemType.LENT);

        if (lentTickets.isEmpty()) {
            throw new ServiceException(ErrorCode.LENT_TICKET_NOT_FOUND);
        }

        ItemHistory ticket = lentTickets.get(0);
        ticket.use();

        lentHistory.extendExpiration((long) lentTerm);

        log.info("수동 연장 성공! 새 만료일: {}", lentHistory.getExpiredAt());
    }

    @Override
    public void useSwap(
            Long userId,
            Integer newVisibleNum,
            String previousPassword,
            MultipartFile file,
            Boolean forceReturn,
            String reason) {
        log.info(
                "이사 시도(AI) - User: {}, NewCabinet: {}, Force: {}",
                userId,
                newVisibleNum,
                forceReturn);

        boolean isAiSuccess = false;
        try {
            isAiSuccess = aiCheckPort.checkItem(file);
        } catch (ServiceException e) {
            if (!e.getErrorCode().equals(ErrorCode.CABINET_NOT_EMPTY)
                    && !(e.getErrorCode().equals(ErrorCode.INVALID_IMAGE) && forceReturn)) {
                throw e;
            }
        }

        if (!isAiSuccess && !forceReturn) {
            throw new ServiceException(ErrorCode.CABINET_NOT_EMPTY);
        }

        String photoUrl = imageUploadPort.uploadImage(userId, file);

        swapWithLocks(
                userId,
                newVisibleNum,
                lockedNewCabinet ->
                        doSwap(
                                userId,
                                newVisibleNum,
                                previousPassword,
                                forceReturn,
                                reason,
                                photoUrl,
                                lockedNewCabinet));
    }

    /**
     * 이사 트랜잭션을 사용자 행 락 -> 옛·새 사물함 행 락 순서로 잠근 뒤 실행한다. 새 사물함만 잠그던 때는 옛 사물함을 관리자의 상태 변경과 서로 덮어썼다(반납과
     * 같은 결함).
     *
     * <p>사물함 두 개를 잠그므로 <b>ID 오름차순</b>으로 잡는다. 관리자 일괄 변경·Undo 도 오름차순이라, 서로의 옛·새 사물함으로 동시에 이사하는 두
     * 유저(A: a->b, B: b->a)도 같은 순서로 잠가 데드락이 나지 않는다. ID 를 락 전에 알아야 하는데 대여 행을 락 전에 읽으면 스냅샷이 잡혀 락 뒤에도 옛
     * 값이 보이므로, 옛 사물함 ID(스칼라)와 새 사물함 ID 를 트랜잭션 밖에서 읽고 락 뒤에 옛 사물함이 그대로인지 확인한다(바뀌었으면 다시 시도). 둘 중 하나라도
     * 없으면 락 없이 본 흐름으로 보내 기존 오류 순서(대여 없음, 사물함 없음)를 그대로 유지한다.
     */
    private void swapWithLocks(Long userId, Integer newVisibleNum, Consumer<Cabinet> body) {
        for (int attempt = 0; attempt < MAX_RETURN_LOCK_ATTEMPTS; attempt++) {
            Long oldCabinetId = lentRepository.findActiveCabinetIdByUserId(userId).orElse(null);
            Long newCabinetId =
                    cabinetRepository
                            .findByVisibleNum(newVisibleNum)
                            .map(Cabinet::getId)
                            .orElse(null);
            try {
                transactionTemplate.execute(
                        status -> {
                            // 이 트랜잭션의 첫 DB 조회여야 한다(REPEATABLE READ 스냅샷 때문).
                            lockUser(userId);
                            Cabinet lockedNewCabinet = null;
                            if (oldCabinetId != null && newCabinetId != null) {
                                lockedNewCabinet =
                                        lockCabinetsInIdOrder(oldCabinetId, newCabinetId);
                                Long lockedOldCabinetId =
                                        lentRepository
                                                .findActiveCabinetIdByUserId(userId)
                                                .orElse(null);
                                if (!oldCabinetId.equals(lockedOldCabinetId)
                                        || !newVisibleNum.equals(
                                                lockedNewCabinet.getVisibleNum())) {
                                    throw new ReturnTargetChangedException();
                                }
                            }
                            body.accept(lockedNewCabinet);
                            return null;
                        });
                return;
            } catch (ReturnTargetChangedException e) {
                log.info("이사 대상 사물함이 바뀌어 다시 시도합니다 - User: {}", userId);
            }
        }
        throw new ServiceException(ErrorCode.REQUEST_IN_PROGRESS);
    }

    /**
     * 사물함 행 락을 PK(ID) 오름차순으로 잡고 두 번째 인자(새 사물함)의 엔티티를 돌려준다.
     *
     * <p>PK 로 잠근 사물함을 같은 트랜잭션에서 {@code findByVisibleNumWithLock} 으로 <b>다시 찾으면 안 된다</b>. {@code
     * VISIBLE_NUM} 에 인덱스가 없는 스키마(엔티티에 인덱스 정의가 없어 Hibernate 가 만든 테스트·데모 DB)에서는 그 쿼리가 PK 순으로 테이블을 훑으며
     * 지나가는 모든 행을 잠가서, 다른 트랜잭션이 쥔 낮은 ID 행을 기다리다 교착이 난다(실제로 재현됨). 그래서 이미 잠근 엔티티를 그대로 넘겨 쓴다.
     */
    private Cabinet lockCabinetsInIdOrder(Long oldId, Long newId) {
        Cabinet locked = null;
        for (Long id : java.util.stream.Stream.of(oldId, newId).distinct().sorted().toList()) {
            Cabinet cabinet =
                    cabinetRepository
                            .findByIdWithLock(id)
                            .orElseThrow(() -> new ServiceException(ErrorCode.CABINET_NOT_FOUND));
            if (id.equals(newId)) {
                locked = cabinet;
            }
        }
        return locked;
    }

    protected void processSwapTransaction(
            Long userId,
            Integer newVisibleNum,
            String previousPassword,
            Boolean forceReturn,
            String reason,
            String photoUrl) {
        doSwap(userId, newVisibleNum, previousPassword, forceReturn, reason, photoUrl, null);
    }

    /** lockedNewCabinet 이 있으면 이미 행 락을 잡은 새 사물함이고, 없으면(null) 여기서 번호로 찾아 잠근다. */
    private void doSwap(
            Long userId,
            Integer newVisibleNum,
            String previousPassword,
            Boolean forceReturn,
            String reason,
            String photoUrl,
            Cabinet lockedNewCabinet) {
        // 같은 유저의 동시 이사를 직렬화한다. 이 트랜잭션의 첫 DB 조회여야 하고(그래야 이후 조회가 앞 요청의 커밋을 본다),
        // 락 순서는 사용자 -> 사물함이다.
        User user = lockUser(userId);

        LentHistory oldLent =
                lentRepository
                        .findByUserIdAndEndedAtIsNull(userId)
                        .orElseThrow(() -> new ServiceException(ErrorCode.LENT_NOT_FOUND));

        if (oldLent.getExpiredAt().toLocalDate().isBefore(java.time.LocalDate.now())) {
            throw new ServiceException(ErrorCode.OVERDUE_USER_CANNOT_SWAP);
        }

        if (user.getPenaltyDays() > 0) {
            throw new ServiceException(ErrorCode.PENALTY_USER);
        }

        if (oldLent.getCabinet().getVisibleNum().equals(newVisibleNum)) {
            throw new ServiceException(ErrorCode.SAME_CABINET_SWAP);
        }

        Cabinet newCabinet =
                lockedNewCabinet != null
                        ? lockedNewCabinet
                        : cabinetRepository
                                .findByVisibleNumWithLock(newVisibleNum)
                                .orElseThrow(
                                        () -> new ServiceException(ErrorCode.CABINET_NOT_FOUND));

        // 사물함 행 락을 잡은 뒤에 예약을 확인한다. 예약(makeReservation)도 같은 행 락 안에서 Redis 에 쓰므로,
        // 확인과 이사 사이에 다른 사람이 예약을 끼워 넣을 수 없다.
        checkCabinetReservation(newVisibleNum, userId);

        if (newCabinet.getStatus() != CabinetStatus.AVAILABLE) {
            throw new ServiceException(ErrorCode.INVALID_CABINET_STATUS);
        }

        validateLentTypePermission(user, newCabinet);

        List<ItemHistory> swapTickets =
                itemHistoryRepository.findUnusedItems(userId, ItemType.SWAP);

        if (swapTickets.isEmpty()) {
            throw new ServiceException(ErrorCode.SWAP_TICKET_NOT_FOUND);
        }

        ItemHistory ticket = swapTickets.get(0);
        ticket.use();

        Cabinet oldCabinet = oldLent.getCabinet();

        String returnReason = previousPassword;
        if (reason != null && !reason.isBlank()) {
            returnReason = forceReturn ? "[User Force] " + reason : "[Swap] " + reason;
        } else if (forceReturn) {
            returnReason = "[User Force] " + previousPassword;
        }

        LocalDateTime swapAt = LocalDateTime.now();
        checkAndApplyPenalty(user, oldLent, swapAt);

        oldLent.endLent(swapAt, returnReason);
        oldLent.attachReturnPhoto(photoUrl);

        if (oldCabinet.getStatus() == CabinetStatus.FULL) {
            oldCabinet.updateStatus(CabinetStatus.AVAILABLE);
            if (forceReturn) {
                oldCabinet.updateStatus(CabinetStatus.PENDING);
            }
        }

        newCabinet.updateStatus(CabinetStatus.FULL);

        LentHistory newLent =
                LentHistory.of(user, newCabinet, LocalDateTime.now(), oldLent.getExpiredAt());
        lentRepository.save(newLent);

        reservationPort.deleteReservation(newVisibleNum, userId);
    }

    @Override
    @Transactional
    public void usePenaltyExemption(Long userId) {
        // 같은 유저의 동시 감면을 직렬화한다(감면권과 패널티 일수를 함께 바꾸므로). 이 트랜잭션의 첫 DB 조회여야 한다.
        User user = lockUser(userId);

        if (user.getPenaltyDays() <= 0) {
            throw new ServiceException(ErrorCode.PENALTY_NOT_FOUND);
        }

        List<ItemHistory> penaltyTickets =
                itemHistoryRepository.findUnusedItems(userId, ItemType.PENALTY_EXEMPTION);

        if (penaltyTickets.isEmpty()) {
            throw new ServiceException(ErrorCode.PENALTY_EXEMPTION_TICKET_NOT_FOUND);
        }

        ItemHistory ticket = penaltyTickets.get(0);
        ticket.use();

        user.decayPenalty();
    }

    @Override
    @Transactional
    public void updateAutoExtensionStatus(Long userId, Boolean enabled) {
        LentHistory lentHistory =
                lentRepository
                        .findByUserIdAndEndedAtIsNull(userId)
                        .orElseThrow(() -> new ServiceException(ErrorCode.LENT_NOT_FOUND));
        lentHistory.setAutoExtension(enabled);
    }

    @Override
    @Transactional
    @DistributedLock(key = "cabinet_lent", identifier = "#visibleNum")
    public void makeReservation(Long userId, Integer visibleNum) {
        log.info("사물함 예약 시도 - User: {}, Cabinet Num: {}", userId, visibleNum);

        // 같은 사물함을 다시 예약하는 것은 DB 작업 전에 빠르게 거부한다. 최종 판단은 아래 reserveReplacing 이 한다.
        if (reservationPort.getUserReservation(userId).filter(visibleNum::equals).isPresent()) {
            throw new ServiceException(ErrorCode.ALREADY_RESERVED);
        }

        boolean isRenting = lentRepository.findByUserIdAndEndedAtIsNull(userId).isPresent();

        if (isRenting) {
            List<ItemHistory> swapTickets =
                    itemHistoryRepository.findUnusedItems(userId, ItemType.SWAP);
            if (swapTickets.isEmpty()) {
                throw new ServiceException(ErrorCode.SWAP_TICKET_NOT_FOUND);
            }
        }

        Cabinet cabinet =
                cabinetRepository
                        .findByVisibleNumWithLock(visibleNum)
                        .orElseThrow(() -> new ServiceException(ErrorCode.CABINET_NOT_FOUND));

        if (cabinet.getStatus() != CabinetStatus.AVAILABLE) {
            throw new ServiceException(ErrorCode.INVALID_CABINET_STATUS);
        }

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));
        validateLentTypePermission(user, cabinet);

        // 위 검증을 모두 통과한 뒤에만 기존 예약을 취소하고 새로 예약한다(실패하면 기존 예약은 그대로).
        // 한 사용자는 예약을 하나만 가지며, 확인과 변경이 Redis 안에서 한 번에 일어나 동시 요청에도 하나만 남는다.
        ReservationOutcome outcome =
                reservationPort.reserveReplacing(visibleNum, userId, RESERVATION_TTL_MINUTES);

        switch (outcome.status()) {
            case TAKEN_BY_OTHER -> throw new ServiceException(ErrorCode.CABINET_ALREADY_RESERVED);
            case ALREADY_MINE -> throw new ServiceException(ErrorCode.ALREADY_RESERVED);
            case RESERVED -> {
                if (outcome.replacedVisibleNum() != null) {
                    log.info(
                            "기존 예약 자동 취소 - User: {}, 취소: {}번, 새 예약: {}번",
                            userId,
                            outcome.replacedVisibleNum(),
                            visibleNum);
                }
                log.info("사물함 예약 성공 - User: {}, Cabinet: {}", userId, visibleNum);
            }
        }
    }

    @Override
    public Integer cancelReservation(Long userId) {
        Integer cancelled =
                reservationPort
                        .cancelUserReservation(userId)
                        .orElseThrow(() -> new ServiceException(ErrorCode.RESERVATION_NOT_FOUND));
        log.info("사물함 예약 취소 - User: {}, Cabinet: {}", userId, cancelled);
        return cancelled;
    }

    /**
     * 사용자 행을 비관적 락으로 가져온다. 같은 유저의 대여/연장/이사 요청이 동시에 들어와도 한 번에 하나씩만 진행된다. 이 호출은 트랜잭션의 첫 DB 조회로 둘 것:
     * REPEATABLE READ 에서는 락을 얻은 뒤의 조회부터 앞 요청의 커밋 결과가 보인다.
     */
    private User lockUser(Long userId) {
        return userRepository
                .findByIdWithLock(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));
    }

    private void checkCabinetReservation(Integer visibleNum, Long userId) {
        var reservedUserId = reservationPort.getReservedUserId(visibleNum);
        if (reservedUserId.isPresent() && !reservedUserId.get().equals(userId)) {
            throw new ServiceException(ErrorCode.CABINET_ALREADY_RESERVED);
        }
    }

    /** 연체였다면 패널티를 부여하고, 이번에 새로 붙인 패널티 일수를 돌려준다. 연체가 아니면 0. */
    private int checkAndApplyPenalty(User user, LentHistory lentHistory, LocalDateTime now) {
        if (!lentHistory.isOverdue(now)) {
            return 0;
        }

        int overdueDays = lentHistory.calculateOverdueDays(now);
        if (overdueDays <= 0) overdueDays = 1;

        int newPenalty = overdueDays * 3;
        user.applyPenalty(newPenalty);

        log.info(
                "연체 패널티 부여: User={}, 연체일={}일, 추가 패널티={}일, 총 패널티={}일",
                user.getName(),
                overdueDays,
                newPenalty,
                user.getPenaltyDays());
        return newPenalty;
    }

    /** 반납 직전 시점(now) 기준의 만료/연체 정보를 만든다. 반드시 endLent 호출 전에 불러야 한다. */
    private LentReturnResult buildReturnResult(
            LentHistory lentHistory, int penaltyAppliedDays, LocalDateTime now) {
        return new LentReturnResult(
                lentHistory.getExpiredAt(),
                lentHistory.calculateRemainingDays(now.toLocalDate()),
                lentHistory.isOverdue(now),
                penaltyAppliedDays,
                now);
    }

    private void validateLentTypePermission(User user, Cabinet cabinet) {
        if (user.isPisciner()) {
            if (cabinet.getLentType() != LentType.LAPISCINE) {
                throw new ServiceException(ErrorCode.PISCINER_CABINET_RESTRICTED);
            }
        } else {
            if (cabinet.getLentType() == LentType.LAPISCINE) {
                throw new ServiceException(ErrorCode.NON_PISCINER_LAPISCINE_RESTRICTED);
            }
        }
    }
}
