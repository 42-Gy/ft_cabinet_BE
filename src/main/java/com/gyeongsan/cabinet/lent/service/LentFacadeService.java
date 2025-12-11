package com.gyeongsan.cabinet.lent.service;

import com.gyeongsan.cabinet.cabinet.domain.Cabinet;
import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;
import com.gyeongsan.cabinet.cabinet.repository.CabinetRepository;
import com.gyeongsan.cabinet.item.domain.ItemHistory;
import com.gyeongsan.cabinet.item.domain.ItemType;
import com.gyeongsan.cabinet.item.repository.ItemHistoryRepository; // 👈 복원
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
    private final ItemHistoryRepository itemHistoryRepository; // 👈 복원

    /**
     * 사물함 대여 시작 (핵심 비즈니스 로직)
     * // 👇 [원래대로] userId를 인자로 받습니다.
     */
    @Transactional
    public void startLentCabinet(Long userId, Long cabinetId) {
        log.info("대여 시도 - User: {}, Cabinet: {}", userId, cabinetId);

        // 1. 유저 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 없습니다."));

        // 👇 락(Lock)을 걸고 사물함 정보를 조회합니다!
        Cabinet cabinet = cabinetRepository.findByIdWithLock(cabinetId)
                .orElseThrow(() -> new IllegalArgumentException("사물함이 없습니다."));

        // 2. [검증] 이미 다른 사물함을 빌리고 있는지 확인
        if (lentRepository.findByUserIdAndEndedAtIsNull(userId).isPresent()) {
            throw new IllegalArgumentException("이미 대여 중인 사물함이 있습니다.");
        }

        // 3. 블랙홀 D-3일 이내 유저 대여 제한 정책
        LocalDateTime blackholedAt = user.getBlackholedAt();
        if (blackholedAt != null && blackholedAt.isBefore(LocalDateTime.now().plusDays(3))) {
            throw new IllegalArgumentException("블랙홀 예정(D-3일 이내) 유저는 대여할 수 없습니다.");
        }

        // 4. [검증] 사물함이 사용 가능 상태인지 확인
        if (cabinet.getStatus() != CabinetStatus.AVAILABLE) {
            throw new IllegalArgumentException("사용할 수 없는 사물함입니다. 상태: " + cabinet.getStatus());
        }

        // 5. [핵심] 유저가 대여권 아이템을 가지고 있는지 확인 (원래대로 복원)
        List<ItemHistory> lentTickets = itemHistoryRepository.findUnusedItems(userId, ItemType.LENT);
        if (lentTickets.isEmpty()) {
            throw new IllegalArgumentException("대여권(ITEM)이 부족합니다! 상점에서 구매해주세요.");
        }
        ItemHistory ticket = lentTickets.get(0);

        // 6. [실행] 대여 처리
        // 6-1. 아이템 사용 처리
        ticket.use();
        // 6-2. 사물함 상태 변경 (AVAILABLE -> FULL)
        cabinet.updateStatus(CabinetStatus.FULL);
        // 6-3. 대여 기록 생성
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = now.plusDays(30);

        LentHistory lentHistory = LentHistory.of(user, cabinet, now, expiredAt);
        lentRepository.save(lentHistory);

        log.info("대여 성공! 대여 ID: {}", lentHistory.getId());
    }

    /**
     * 사물함 반납 (대여 종료)
     */
    @Transactional
    public void endLentCabinet(Long userId) {
        log.info("반납 시도 - User: {}", userId);

        // 1. [검증] 현재 빌리고 있는 사물함이 있는지 확인
        LentHistory lentHistory = lentRepository.findByUserIdAndEndedAtIsNull(userId)
                .orElseThrow(() -> new IllegalArgumentException("현재 대여 중인 사물함이 없습니다."));

        Cabinet cabinet = lentHistory.getCabinet();

        // 2. [실행] 반납 처리
        // 2-1. 대여 기록 종료 (반납 시간 기록)
        lentHistory.endLent(LocalDateTime.now());

        // 2-2. 사물함 상태 변경 (FULL -> AVAILABLE)
        // 단, 고장(BROKEN) 상태일 때는 AVAILABLE로 바꾸지 않음.
        if (cabinet.getStatus() == CabinetStatus.FULL) {
            cabinet.updateStatus(CabinetStatus.AVAILABLE);
        }
        log.info("반납 성공! 대여 ID: {}, 사물함 ID: {}", lentHistory.getId(), cabinet.getId());
    }
}