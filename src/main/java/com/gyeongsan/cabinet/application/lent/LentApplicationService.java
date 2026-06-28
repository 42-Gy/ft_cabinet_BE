package com.gyeongsan.cabinet.application.lent;

import com.gyeongsan.cabinet.cabinet.domain.Cabinet;
import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;
import com.gyeongsan.cabinet.cabinet.domain.LentType;
import com.gyeongsan.cabinet.domain.cabinet.port.out.CabinetRepositoryPort;
import com.gyeongsan.cabinet.domain.item.port.out.ItemHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.in.LentUseCase;
import com.gyeongsan.cabinet.domain.lent.port.out.AiCheckPort;
import com.gyeongsan.cabinet.domain.lent.port.out.ImageUploadPort;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.ReservationPort;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import com.gyeongsan.cabinet.item.domain.ItemHistory;
import com.gyeongsan.cabinet.item.domain.ItemType;
import com.gyeongsan.cabinet.lent.domain.LentHistory;
import com.gyeongsan.cabinet.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

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

    @Value("${cabinet.policy.lent-term}")
    private int lentTerm;

    @Value("${cabinet.policy.extension-term}")
    private long extensionTerm;

    @Override
    @Transactional
    public void startLent(Long userId, Integer visibleNum) {
        log.info("대여 시도 - User: {}, Cabinet Num: {}", userId, visibleNum);

        checkCabinetReservation(visibleNum, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        if (user.getPenaltyDays() > 0) {
            throw new ServiceException(ErrorCode.PENALTY_USER);
        }

        Cabinet cabinet = cabinetRepository.findByVisibleNumWithLock(visibleNum)
                .orElseThrow(() -> new ServiceException(ErrorCode.CABINET_NOT_FOUND));

        if (lentRepository.findByUserIdAndEndedAtIsNull(userId).isPresent()) {
            throw new ServiceException(ErrorCode.LENT_ALREADY_EXIST);
        }

        if (cabinet.getStatus() != CabinetStatus.AVAILABLE) {
            throw new ServiceException(ErrorCode.INVALID_CABINET_STATUS);
        }

        validateLentTypePermission(user, cabinet);

        List<ItemHistory> lentTickets = itemHistoryRepository.findUnusedItems(userId, ItemType.LENT);

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
    public void endLent(Long userId, String previousPassword, MultipartFile file, Boolean forceReturn, String reason) {
        log.info("AI 반납 시도 - User: {}, Force: {}, Reason: {}", userId, forceReturn, reason);

        boolean isAiSuccess = false;
        try {
            isAiSuccess = aiCheckPort.checkItem(file);
        } catch (ServiceException e) {
            if (!e.getErrorCode().equals(ErrorCode.CABINET_NOT_EMPTY) &&
                    !(e.getErrorCode().equals(ErrorCode.INVALID_IMAGE) && forceReturn)) {
                throw e;
            }
        }

        if (!isAiSuccess && !forceReturn) {
            throw new ServiceException(ErrorCode.CABINET_NOT_EMPTY);
        }

        boolean doManualReturn = !isAiSuccess && forceReturn;
        String photoUrl = imageUploadPort.uploadImage(userId, file);

        transactionTemplate.execute(status -> {
            if (doManualReturn) {
                String returnReason = (reason != null && !reason.isBlank()) ? "[User Force] " + reason
                        : "AI 검사 실패 및 강제 반납";
                endLentManual(userId, previousPassword, returnReason, photoUrl);
            } else {
                processReturnTransaction(userId, previousPassword, photoUrl);
            }
            return null;
        });
    }

    @Transactional
    public void endLentManual(Long userId, String previousPassword, String reason, String photoUrl) {
        LentHistory lentHistory = lentRepository.findByUserIdAndEndedAtIsNull(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.LENT_NOT_FOUND));

        User user = lentHistory.getUser();
        checkAndApplyPenalty(user, lentHistory);

        lentHistory.endLent(LocalDateTime.now(), previousPassword);
        lentHistory.setPhotoUrl(photoUrl);

        Cabinet cabinet = lentHistory.getCabinet();
        cabinet.updateStatus(CabinetStatus.PENDING);
        cabinet.updateStatusNote(reason);
    }

    @Transactional
    protected void processReturnTransaction(Long userId, String previousPassword, String photoUrl) {
        LentHistory lentHistory = lentRepository.findByUserIdAndEndedAtIsNull(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.LENT_NOT_FOUND));

        User user = lentHistory.getUser();
        checkAndApplyPenalty(user, lentHistory);

        Cabinet cabinet = lentHistory.getCabinet();

        lentHistory.endLent(LocalDateTime.now(), previousPassword);
        lentHistory.setPhotoUrl(photoUrl);

        if (cabinet.getStatus() == CabinetStatus.FULL || cabinet.getStatus() == CabinetStatus.OVERDUE) {
            cabinet.updateStatus(CabinetStatus.AVAILABLE);
        }

        log.info("반납 성공! 대여 ID: {}, 사물함: {}", lentHistory.getId(), cabinet.getVisibleNum());
    }

    @Override
    @Transactional
    public void useExtension(Long userId) {
        log.info("연장권 사용 시도 - User: {}", userId);

        userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        LentHistory lentHistory = lentRepository.findByUserIdAndEndedAtIsNull(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.LENT_NOT_FOUND));

        List<ItemHistory> extensionTickets = itemHistoryRepository.findUnusedItems(userId, ItemType.EXTENSION);

        if (extensionTickets.isEmpty()) {
            throw new ServiceException(ErrorCode.EXTENSION_TICKET_NOT_FOUND);
        }

        User user = userRepository.findById(userId)
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

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        if (user.getPenaltyDays() > 0) {
            throw new ServiceException(ErrorCode.PENALTY_USER);
        }

        if (user.isPisciner()) {
            throw new ServiceException(ErrorCode.PISCINER_EXTENSION_RESTRICTED);
        }

        LentHistory lentHistory = lentRepository.findByUserIdAndEndedAtIsNull(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.LENT_NOT_FOUND));

        List<ItemHistory> lentTickets = itemHistoryRepository.findUnusedItems(userId, ItemType.LENT);

        if (lentTickets.isEmpty()) {
            throw new ServiceException(ErrorCode.LENT_TICKET_NOT_FOUND);
        }

        ItemHistory ticket = lentTickets.get(0);
        ticket.use();

        lentHistory.extendExpiration((long) lentTerm);

        log.info("수동 연장 성공! 새 만료일: {}", lentHistory.getExpiredAt());
    }

    @Override
    public void useSwap(Long userId, Integer newVisibleNum, String previousPassword, MultipartFile file,
            Boolean forceReturn, String reason) {
        log.info("이사 시도(AI) - User: {}, NewCabinet: {}, Force: {}", userId, newVisibleNum, forceReturn);

        boolean isAiSuccess = false;
        try {
            isAiSuccess = aiCheckPort.checkItem(file);
        } catch (ServiceException e) {
            if (!e.getErrorCode().equals(ErrorCode.CABINET_NOT_EMPTY) &&
                    !(e.getErrorCode().equals(ErrorCode.INVALID_IMAGE) && forceReturn)) {
                throw e;
            }
        }

        if (!isAiSuccess && !forceReturn) {
            throw new ServiceException(ErrorCode.CABINET_NOT_EMPTY);
        }

        String photoUrl = imageUploadPort.uploadImage(userId, file);

        checkCabinetReservation(newVisibleNum, userId);

        transactionTemplate.execute(status -> {
            processSwapTransaction(userId, newVisibleNum, previousPassword, forceReturn, reason, photoUrl);
            return null;
        });
    }

    protected void processSwapTransaction(Long userId, Integer newVisibleNum, String previousPassword,
            Boolean forceReturn, String reason, String photoUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        LentHistory oldLent = lentRepository.findByUserIdAndEndedAtIsNull(userId)
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

        Cabinet newCabinet = cabinetRepository.findByVisibleNumWithLock(newVisibleNum)
                .orElseThrow(() -> new ServiceException(ErrorCode.CABINET_NOT_FOUND));

        if (newCabinet.getStatus() != CabinetStatus.AVAILABLE) {
            throw new ServiceException(ErrorCode.INVALID_CABINET_STATUS);
        }

        validateLentTypePermission(user, newCabinet);

        List<ItemHistory> swapTickets = itemHistoryRepository.findUnusedItems(userId, ItemType.SWAP);

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

        checkAndApplyPenalty(user, oldLent);

        oldLent.endLent(LocalDateTime.now(), returnReason);
        oldLent.setPhotoUrl(photoUrl);

        if (oldCabinet.getStatus() == CabinetStatus.FULL) {
            oldCabinet.updateStatus(CabinetStatus.AVAILABLE);
            if (forceReturn) {
                oldCabinet.updateStatus(CabinetStatus.PENDING);
            }
        }

        newCabinet.updateStatus(CabinetStatus.FULL);

        LentHistory newLent = LentHistory.of(user, newCabinet, LocalDateTime.now(), oldLent.getExpiredAt());
        lentRepository.save(newLent);

        reservationPort.deleteReservation(newVisibleNum, userId);
    }

    @Override
    @Transactional
    public void usePenaltyExemption(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        if (user.getPenaltyDays() <= 0) {
            throw new ServiceException(ErrorCode.PENALTY_NOT_FOUND);
        }

        List<ItemHistory> penaltyTickets = itemHistoryRepository.findUnusedItems(userId, ItemType.PENALTY_EXEMPTION);

        if (penaltyTickets.isEmpty()) {
            throw new ServiceException(ErrorCode.PENALTY_EXEMPTION_TICKET_NOT_FOUND);
        }

        ItemHistory ticket = penaltyTickets.get(0);
        ticket.use();

        int newPenalty = user.getPenaltyDays() - 1;
        user.updatePenaltyDays(newPenalty);
    }

    @Override
    @Transactional
    public void processBlackholeReturn(Long userId) {
        LentHistory lentHistory = lentRepository.findByUserIdAndEndedAtIsNull(userId).orElse(null);

        if (lentHistory == null) {
            return;
        }

        Cabinet cabinet = lentHistory.getCabinet();
        lentHistory.endLent(LocalDateTime.now(), "블랙홀(퇴소) 반납 보류");

        if (cabinet.getStatus() == CabinetStatus.FULL) {
            cabinet.updateStatus(CabinetStatus.PENDING);
        }
    }

    @Override
    @Transactional
    public void updateAutoExtensionStatus(Long userId, Boolean enabled) {
        LentHistory lentHistory = lentRepository.findByUserIdAndEndedAtIsNull(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.LENT_NOT_FOUND));
        lentHistory.setAutoExtension(enabled);
    }

    @Override
    @Transactional
    public void makeReservation(Long userId, Integer visibleNum) {
        log.info("사물함 예약 시도 - User: {}, Cabinet Num: {}", userId, visibleNum);

        if (reservationPort.getUserReservation(userId).isPresent()) {
            throw new ServiceException(ErrorCode.ALREADY_RESERVED);
        }

        boolean isRenting = lentRepository.findByUserIdAndEndedAtIsNull(userId).isPresent();

        if (isRenting) {
            List<ItemHistory> swapTickets = itemHistoryRepository.findUnusedItems(userId, ItemType.SWAP);
            if (swapTickets.isEmpty()) {
                throw new ServiceException(ErrorCode.SWAP_TICKET_NOT_FOUND);
            }
        }

        Cabinet cabinet = cabinetRepository.findByVisibleNumWithLock(visibleNum)
                .orElseThrow(() -> new ServiceException(ErrorCode.CABINET_NOT_FOUND));

        if (cabinet.getStatus() != CabinetStatus.AVAILABLE) {
            throw new ServiceException(ErrorCode.INVALID_CABINET_STATUS);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));
        validateLentTypePermission(user, cabinet);

        var reservedUserId = reservationPort.getReservedUserId(visibleNum);
        if (reservedUserId.isPresent() && !reservedUserId.get().equals(userId)) {
            throw new ServiceException(ErrorCode.CABINET_ALREADY_RESERVED);
        }

        reservationPort.reserve(visibleNum, userId, 15);

        log.info("사물함 예약 성공 - User: {}, Cabinet: {}", userId, visibleNum);
    }

    private void checkCabinetReservation(Integer visibleNum, Long userId) {
        var reservedUserId = reservationPort.getReservedUserId(visibleNum);
        if (reservedUserId.isPresent() && !reservedUserId.get().equals(userId)) {
            throw new ServiceException(ErrorCode.CABINET_ALREADY_RESERVED);
        }
    }

    private void checkAndApplyPenalty(User user, LentHistory lentHistory) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = lentHistory.getExpiredAt();

        if (now.isBefore(expiredAt) || now.isEqual(expiredAt)) {
            return;
        }

        long overdueDays = ChronoUnit.DAYS.between(expiredAt.toLocalDate(), now.toLocalDate());

        if (overdueDays <= 0) {
            overdueDays = 1;
        }

        int newPenalty = (int) (overdueDays * 3);
        int currentPenalty = user.getPenaltyDays();
        int totalPenalty = currentPenalty + newPenalty;

        user.updatePenaltyDays(totalPenalty);

        log.info("연체 패널티 부여: User={}, 연체일={}일, 추가 패널티={}일, 총 패널티={}일",
                user.getName(), overdueDays, newPenalty, totalPenalty);
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
