package com.gyeongsan.cabinet.lent.service;

import com.gyeongsan.cabinet.cabinet.domain.Cabinet;
import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;
import com.gyeongsan.cabinet.cabinet.repository.CabinetRepository;
import com.gyeongsan.cabinet.item.domain.ItemHistory;
import com.gyeongsan.cabinet.item.domain.ItemType;
import com.gyeongsan.cabinet.item.repository.ItemHistoryRepository;
import com.gyeongsan.cabinet.lent.domain.LentHistory;
import com.gyeongsan.cabinet.lent.repository.LentRepository;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class LentFacadeService {

    private final UserRepository userRepository;
    private final CabinetRepository cabinetRepository;
    private final LentRepository lentRepository;
    private final ItemHistoryRepository itemHistoryRepository;

    @Transactional
    public void startLentCabinet(Long userId, Long cabinetId) {
        log.info("대여 시도 - User: {}, Cabinet: {}", userId, cabinetId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 없습니다."));

        if (user.getPenaltyDays() > 0) {
            throw new IllegalArgumentException(
                    "🚫 패널티 기간입니다! " + user.getPenaltyDays() + "일 뒤에 대여 가능합니다."
            );
        }

        Cabinet cabinet = cabinetRepository.findByIdWithLock(cabinetId)
                .orElseThrow(() -> new IllegalArgumentException("사물함이 없습니다."));

        if (lentRepository.findByUserIdAndEndedAtIsNull(userId).isPresent()) {
            throw new IllegalArgumentException("이미 대여 중인 사물함이 있습니다.");
        }

        LocalDateTime blackholedAt = user.getBlackholedAt();
        if (blackholedAt != null && blackholedAt.isBefore(LocalDateTime.now().plusDays(3))) {
            throw new IllegalArgumentException("블랙홀 예정(D-3일 이내) 유저는 대여할 수 없습니다.");
        }

        if (cabinet.getStatus() != CabinetStatus.AVAILABLE) {
            throw new IllegalArgumentException(
                    "사용할 수 없는 사물함입니다. 상태: " + cabinet.getStatus()
            );
        }

        List<ItemHistory> lentTickets =
                itemHistoryRepository.findUnusedItems(userId, ItemType.LENT);

        if (lentTickets.isEmpty()) {
            throw new IllegalArgumentException(
                    "대여권(ITEM)이 부족합니다! 상점에서 구매해주세요."
            );
        }

        ItemHistory ticket = lentTickets.get(0);
        ticket.use();

        cabinet.updateStatus(CabinetStatus.FULL);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = now.plusDays(30);

        LentHistory lentHistory = LentHistory.of(user, cabinet, now, expiredAt);
        lentRepository.save(lentHistory);

        log.info("대여 성공! 대여 ID: {}", lentHistory.getId());
    }

    @Transactional
    public void endLentCabinet(Long userId) {
        log.info("반납 시도 - User: {}", userId);

        LentHistory lentHistory = lentRepository.findByUserIdAndEndedAtIsNull(userId)
                .orElseThrow(() -> new IllegalArgumentException("현재 대여 중인 사물함이 없습니다."));

        Cabinet cabinet = lentHistory.getCabinet();

        lentHistory.endLent(LocalDateTime.now());

        if (cabinet.getStatus() == CabinetStatus.FULL) {
            cabinet.updateStatus(CabinetStatus.AVAILABLE);
        }

        log.info(
                "반납 성공! 대여 ID: {}, 사물함 ID: {}",
                lentHistory.getId(),
                cabinet.getId()
        );
    }

    @Transactional
    public void useExtension(Long userId) {
        log.info("연장권 사용 시도 - User: {}", userId);

        LentHistory lentHistory = lentRepository.findByUserIdAndEndedAtIsNull(userId)
                .orElseThrow(() -> new IllegalArgumentException("현재 대여 중인 사물함이 없습니다."));

        List<ItemHistory> extensionTickets =
                itemHistoryRepository.findUnusedItems(userId, ItemType.EXTENSION);

        if (extensionTickets.isEmpty()) {
            throw new IllegalArgumentException("연장권(ITEM)이 없습니다! 상점에서 구매해주세요.");
        }

        ItemHistory ticket = extensionTickets.get(0);
        ticket.use();

        lentHistory.extendExpiration(15L);

        log.info("연장 성공! 변경된 만료일: {}", lentHistory.getExpiredAt());
    }

    @Transactional
    public void useSwap(Long userId, Long newCabinetId) {
        log.info("이사권 사용 시도 - User: {}, NewCabinet: {}", userId, newCabinetId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 없습니다."));

        LentHistory oldLent = lentRepository.findByUserIdAndEndedAtIsNull(userId)
                .orElseThrow(() -> new IllegalArgumentException("현재 대여 중인 사물함이 없습니다. 이사할 수 없습니다."));

        if (oldLent.getCabinet().getId().equals(newCabinetId)) {
            throw new IllegalArgumentException("현재 사용 중인 사물함과 같은 곳으로 이사할 수 없습니다.");
        }

        Cabinet newCabinet = cabinetRepository.findByIdWithLock(newCabinetId)
                .orElseThrow(() -> new IllegalArgumentException("이사할 사물함이 존재하지 않습니다."));

        if (newCabinet.getStatus() != CabinetStatus.AVAILABLE) {
            throw new IllegalArgumentException("이사할 사물함이 사용 불가능한 상태입니다.");
        }

        List<ItemHistory> swapTickets =
                itemHistoryRepository.findUnusedItems(userId, ItemType.SWAP);

        if (swapTickets.isEmpty()) {
            throw new IllegalArgumentException("이사권(ITEM)이 없습니다! 상점에서 구매해주세요.");
        }

        ItemHistory ticket = swapTickets.get(0);
        ticket.use();

        Cabinet oldCabinet = oldLent.getCabinet();
        oldLent.endLent(LocalDateTime.now());

        if (oldCabinet.getStatus() == CabinetStatus.FULL) {
            oldCabinet.updateStatus(CabinetStatus.AVAILABLE);
        }

        newCabinet.updateStatus(CabinetStatus.FULL);

        LentHistory newLent = LentHistory.of(
                user,
                newCabinet,
                LocalDateTime.now(),
                oldLent.getExpiredAt()
        );
        lentRepository.save(newLent);

        log.info(
                "이사 성공! 🚚 Old: {} -> New: {}, 만료일: {}",
                oldCabinet.getId(),
                newCabinet.getId(),
                newLent.getExpiredAt()
        );
    }

    // 👇 [추가] 패널티 감면권 사용 로직 (패널티 -2일)
    @Transactional
    public void usePenaltyExemption(Long userId) {
        log.info("패널티 감면권 사용 시도 - User: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 없습니다."));

        if (user.getPenaltyDays() <= 0) {
            throw new IllegalArgumentException("현재 적용된 패널티가 없습니다! 아이템을 아껴두세요. 😊");
        }

        List<ItemHistory> penaltyTickets =
                itemHistoryRepository.findUnusedItems(userId, ItemType.PENALTY_EXEMPTION);

        if (penaltyTickets.isEmpty()) {
            throw new IllegalArgumentException("패널티 감면권(ITEM)이 없습니다! 상점에서 구매해주세요.");
        }

        ItemHistory ticket = penaltyTickets.get(0);
        ticket.use();

        int newPenalty = user.getPenaltyDays() - 2;
        user.updatePenaltyDays(newPenalty);

        log.info("감면 성공! 패널티: {}일 -> {}일", newPenalty + 2, user.getPenaltyDays());
    }
}