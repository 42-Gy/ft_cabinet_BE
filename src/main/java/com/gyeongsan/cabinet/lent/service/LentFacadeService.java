package com.gyeongsan.cabinet.lent.service;

import com.gyeongsan.cabinet.cabinet.domain.Cabinet;
import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;
import com.gyeongsan.cabinet.cabinet.repository.CabinetRepository;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import com.gyeongsan.cabinet.item.domain.ItemHistory;
import com.gyeongsan.cabinet.item.domain.ItemType;
import com.gyeongsan.cabinet.item.repository.ItemHistoryRepository;
import com.gyeongsan.cabinet.lent.domain.LentHistory;
import com.gyeongsan.cabinet.lent.repository.LentRepository;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Log4j2
public class LentFacadeService {

    private final UserRepository userRepository;
    private final CabinetRepository cabinetRepository;
    private final LentRepository lentRepository;
    private final ItemHistoryRepository itemHistoryRepository;
    private final TransactionTemplate transactionTemplate;
    private final StringRedisTemplate redisTemplate;

    private final ItemCheckService itemCheckService;
    private final ImageUploadService imageUploadService;

    @Value("${cabinet.policy.lent-term}")
    private int lentTerm;

    @Value("${cabinet.policy.extension-term}")
    private long extensionTerm;

    private static final String RESERVATION_KEY_PREFIX = "cabinet:reservation:";

    @Transactional
    public void startLentCabinet(Long userId, Integer visibleNum) {
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

        deleteReservation(visibleNum);

        log.info("대여 성공! 대여 ID: {}", lentHistory.getId());
    }

    public void checkLentCabinetImage(Long userId, MultipartFile file) {
        log.info("AI 반납 전 선검증 시도 - User: {}, File: {}", userId, file.getOriginalFilename());
        boolean isClean = itemCheckService.checkItem(file);
        if (!isClean) {
            log.warn("AI 선검증 실패 (짐 감지) - User: {}", userId);
            throw new ServiceException(ErrorCode.CABINET_NOT_EMPTY);
        }
        log.info("AI 선검증 성공 (Clean) - User: {}", userId);
    }

    public void endLentCabinet(Long userId, String previousPassword, MultipartFile file, Boolean forceReturn,
            String reason) {
        log.info("AI 반납 시도 - User: {}, Next Password: {}, Force: {}, Reason: {}", userId, previousPassword, forceReturn,
                reason);

        boolean isAiSuccess = false;
        try {
            isAiSuccess = itemCheckService.checkItem(file);
        } catch (ServiceException e) {
            if (!e.getErrorCode().equals(ErrorCode.CABINET_NOT_EMPTY) &&
                    !(e.getErrorCode().equals(ErrorCode.INVALID_IMAGE) && forceReturn)) {
                throw e;
            }
        }

        if (!isAiSuccess && !forceReturn) {
            log.warn("AI 검사 실패 (짐 감지) - User: {}", userId);
            throw new ServiceException(ErrorCode.CABINET_NOT_EMPTY);
        }

        boolean doManualReturn = !isAiSuccess && forceReturn;

        String photoUrl = imageUploadService.uploadImage(userId, file);

        transactionTemplate.execute(status -> {
            if (doManualReturn) {
                String returnReason = (reason != null && !reason.isBlank()) ? "[User Force] " + reason
                        : "AI 검사 실패 및 강제 반납";
                endLentCabinetManual(userId, previousPassword, returnReason, photoUrl);
            } else {
                processReturnTransaction(userId, previousPassword, photoUrl);
            }
            return null;
        });
    }

    @Transactional
    public void endLentCabinetManual(Long userId, String previousPassword, String reason, String photoUrl) {
        log.info("수동 반납 요청 - User: {}, Password: {}, Reason: {}", userId, previousPassword, reason);

        LentHistory lentHistory = lentRepository.findByUserIdAndEndedAtIsNull(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.LENT_NOT_FOUND));

        User user = lentHistory.getUser();
        checkAndApplyPenalty(user, lentHistory);

        lentHistory.endLent(LocalDateTime.now(), previousPassword);
        lentHistory.setPhotoUrl(photoUrl);

        Cabinet cabinet = lentHistory.getCabinet();
        cabinet.updateStatus(CabinetStatus.PENDING);
        cabinet.updateStatusNote(reason);

        log.info("수동 반납 완료. 사물함 {}번 상태 -> PENDING", cabinet.getVisibleNum());
    }

    protected void processReturnTransaction(Long userId, String previousPassword, String photoUrl) {
        LentHistory lentHistory = lentRepository.findByUserIdAndEndedAtIsNull(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.LENT_NOT_FOUND));

        User user = lentHistory.getUser();
        checkAndApplyPenalty(user, lentHistory);

        Cabinet cabinet = lentHistory.getCabinet();

        lentHistory.endLent(LocalDateTime.now(), previousPassword);
        lentHistory.setPhotoUrl(photoUrl);

        if (cabinet.getStatus() == CabinetStatus.FULL) {
            cabinet.updateStatus(CabinetStatus.AVAILABLE);
        }

        log.info("반납 성공! 대여 ID: {}, 사물함: {}, 저장된 비번: {}",
                lentHistory.getId(), cabinet.getVisibleNum(), previousPassword);
    }

    @Transactional
    public void useExtension(Long userId) {
        log.info("연장권 사용 시도 - User: {}", userId);

        LentHistory lentHistory = lentRepository.findByUserIdAndEndedAtIsNull(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.LENT_NOT_FOUND));

        List<ItemHistory> extensionTickets = itemHistoryRepository.findUnusedItems(userId, ItemType.EXTENSION);

        if (extensionTickets.isEmpty()) {
            throw new ServiceException(ErrorCode.EXTENSION_TICKET_NOT_FOUND);
        }

        ItemHistory ticket = extensionTickets.get(0);
        ticket.use();

        lentHistory.extendExpiration(extensionTerm);

        log.info("연장 성공! 변경된 만료일: {}", lentHistory.getExpiredAt());
    }

    public void useSwap(Long userId, Integer newVisibleNum, String previousPassword, MultipartFile file,
            Boolean forceReturn, String reason) {
        log.info("이사 시도(AI) - User: {}, NewCabinet: {}, Force: {}, Reason: {}", userId, newVisibleNum, forceReturn,
                reason);

        boolean isAiSuccess = false;
        try {
            isAiSuccess = itemCheckService.checkItem(file);
        } catch (ServiceException e) {
            if (!e.getErrorCode().equals(ErrorCode.CABINET_NOT_EMPTY) &&
                    !(e.getErrorCode().equals(ErrorCode.INVALID_IMAGE) && forceReturn)) {
                throw e;
            }
        }

        if (!isAiSuccess && !forceReturn) {
            log.warn("AI 이사 검사 실패 - User: {}", userId);
            throw new ServiceException(ErrorCode.CABINET_NOT_EMPTY);
        }

        String photoUrl = imageUploadService.uploadImage(userId, file);

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

        // 연체 상태에서는 이사 불가 (새 사물함 획득 방지)
        if (oldLent.getExpiredAt().toLocalDate().isBefore(java.time.LocalDate.now())) {
            throw new ServiceException(ErrorCode.OVERDUE_USER_CANNOT_SWAP);
        }

        if (oldLent.getCabinet().getVisibleNum().equals(newVisibleNum)) {
            throw new ServiceException(ErrorCode.SAME_CABINET_SWAP);
        }

        Cabinet newCabinet = cabinetRepository.findByVisibleNumWithLock(newVisibleNum)
                .orElseThrow(() -> new ServiceException(ErrorCode.CABINET_NOT_FOUND));

        if (newCabinet.getStatus() != CabinetStatus.AVAILABLE) {
            throw new ServiceException(ErrorCode.INVALID_CABINET_STATUS);
        }

        List<ItemHistory> swapTickets = itemHistoryRepository.findUnusedItems(userId, ItemType.SWAP);

        if (swapTickets.isEmpty()) {
            throw new ServiceException(ErrorCode.SWAP_TICKET_NOT_FOUND);
        }

        ItemHistory ticket = swapTickets.get(0);
        ticket.use();

        Cabinet oldCabinet = oldLent.getCabinet();

        String returnReason = previousPassword;
        if (reason != null && !reason.isBlank()) {
            returnReason = forceReturn
                    ? "[User Force] " + reason
                    : "[Swap] " + reason;
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

        log.info("이사 성공! 🚚 Old(PW:{}): {} -> New: {}", previousPassword, oldCabinet.getVisibleNum(),
                newCabinet.getVisibleNum());

        deleteReservation(newVisibleNum);
    }

    @Transactional
    public void usePenaltyExemption(Long userId) {
        log.info("패널티 감면권 사용 시도 - User: {}", userId);

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

        log.info("감면 성공! 패널티: {}일 -> {}일", newPenalty + 1, user.getPenaltyDays());
    }

    @Transactional
    public void processBlackholeReturn(Long userId) {
        log.info("🪐 블랙홀 유저 반납 처리 시작 - User: {}", userId);

        LentHistory lentHistory = lentRepository.findByUserIdAndEndedAtIsNull(userId)
                .orElse(null);

        if (lentHistory == null) {
            return;
        }

        Cabinet cabinet = lentHistory.getCabinet();

        lentHistory.endLent(LocalDateTime.now(), "블랙홀(퇴소) 반납 보류");

        if (cabinet.getStatus() == CabinetStatus.FULL) {
            cabinet.updateStatus(CabinetStatus.PENDING);
        }

        log.info("처리 완료: 사물함 {}번 상태 -> PENDING", cabinet.getVisibleNum());
    }

    @Transactional
    public void updateAutoExtensionStatus(Long userId, Boolean enabled) {
        LentHistory lentHistory = lentRepository.findByUserIdAndEndedAtIsNull(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.LENT_NOT_FOUND));

        lentHistory.setAutoExtension(enabled);
        log.info("자동 연장 설정 변경 - User: {}, Enabled: {}", userId, enabled);
    }

    @Transactional
    public void makeReservation(Long userId, Integer visibleNum) {
        log.info("사물함 예약 시도 - User: {}, Cabinet Num: {}", userId, visibleNum);

        // 이미 대여 중인 사용자는 예약 불가
        if (lentRepository.findByUserIdAndEndedAtIsNull(userId).isPresent()) {
            throw new ServiceException(ErrorCode.ALREADY_RENTING_CANNOT_RESERVE);
        }

        Cabinet cabinet = cabinetRepository.findByVisibleNumWithLock(visibleNum)
                .orElseThrow(() -> new ServiceException(ErrorCode.CABINET_NOT_FOUND));

        if (cabinet.getStatus() != CabinetStatus.AVAILABLE) {
            throw new ServiceException(ErrorCode.INVALID_CABINET_STATUS);
        }

        String key = RESERVATION_KEY_PREFIX + visibleNum;
        String reservedUserId = redisTemplate.opsForValue().get(key);

        if (reservedUserId != null && !reservedUserId.equals(userId.toString())) {
            throw new ServiceException(ErrorCode.CABINET_ALREADY_RESERVED);
        }

        redisTemplate.opsForValue().set(key, userId.toString(), 15, TimeUnit.MINUTES);
        log.info("사물함 예약 성공 - User: {}, Cabinet: {}", userId, visibleNum);
    }

    private void checkCabinetReservation(Integer visibleNum, Long userId) {
        String key = RESERVATION_KEY_PREFIX + visibleNum;
        String reservedUserId = redisTemplate.opsForValue().get(key);

        if (reservedUserId != null && !reservedUserId.equals(userId.toString())) {
            log.warn("❌ 대여/이사 실패: 다른 사용자가 예약함 - Owner: {}, Requester: {}", reservedUserId, userId);
            throw new ServiceException(ErrorCode.CABINET_ALREADY_RESERVED);
        }
    }

    private void deleteReservation(Integer visibleNum) {
        String key = RESERVATION_KEY_PREFIX + visibleNum;
        redisTemplate.delete(key);
    }

    /**
     * 반납 시점에 연체 여부를 확인하고 즉시 패널티를 부여합니다.
     */
    private void checkAndApplyPenalty(User user, LentHistory lentHistory) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = lentHistory.getExpiredAt();

        // Grace period: 만료일 당일 23:59:59까지는 연체 아님
        LocalDateTime gracePeriodEnd = expiredAt.toLocalDate().atTime(23, 59, 59);

        if (now.isBefore(gracePeriodEnd) || now.isEqual(gracePeriodEnd)) {
            return; // 연체 아님
        }

        // 연체일 계산: 만료일 다음날부터 오늘까지 (날짜 기준)
        // 예: 만료일 1/27, 오늘 1/28 → 1일 연체
        long overdueDays = ChronoUnit.DAYS.between(
                expiredAt.toLocalDate(),
                now.toLocalDate());

        if (overdueDays <= 0) {
            return;
        }

        int penalty = (int) (overdueDays * 3);
        user.updatePenaltyDays(penalty);

        log.info("🚨 연체 패널티 즉시 부여: User={}, 연체일={}일, 패널티={}일",
                user.getName(), overdueDays, penalty);
    }
}
