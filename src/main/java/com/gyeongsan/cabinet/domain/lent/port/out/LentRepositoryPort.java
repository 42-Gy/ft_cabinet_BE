package com.gyeongsan.cabinet.domain.lent.port.out;

import com.gyeongsan.cabinet.domain.lent.model.LentHistory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LentRepositoryPort {

    Optional<LentHistory> findByUserIdAndEndedAtIsNull(Long userId);

    /**
     * 사용자의 활성 대여가 있는 사물함 ID. 사물함 행 락을 잡으려면 ID 를 먼저 알아야 하는데, 대여 행을 읽으면 그 시점에 스냅샷이 잡혀 락을 얻은 뒤에도 옛 값이
     * 보이므로 ID 만 가볍게 읽는 용도다(락이나 엔티티 로딩 없음).
     */
    Optional<Long> findActiveCabinetIdByUserId(Long userId);

    Optional<LentHistory> findByCabinetIdAndEndedAtIsNull(Long cabinetId);

    Optional<LentHistory> findTopByCabinetIdAndEndedAtIsNotNullOrderByEndedAtDesc(Long cabinetId);

    long countByEndedAtIsNull();

    List<LentHistory> findAllOverdueLentHistories(LocalDateTime now);

    List<LentHistory> findAllActiveLentByCabinetIds(List<Long> cabinetIds);

    /** 사용자와 사물함을 함께 가져온다. 없는 ID 는 결과에서 빠진다. */
    List<LentHistory> findAllByIds(List<Long> ids);

    List<LentHistory> findAllActiveLentByUserIds(List<Long> userIds);

    List<LentHistory> findAllActiveLentsByExpiredAtBetween(LocalDateTime start, LocalDateTime end);

    List<LentHistory> findAllActiveLents();

    List<LentHistory> findAllLatestLentForPendingCabinets();

    List<LentHistory> findRecentExpiredActiveLents(LocalDateTime start, LocalDateTime end);

    Page<LentHistory> findHistoryByCabinet(Integer visibleNum, Pageable pageable);

    Page<LentHistory> findAllReturnedWithPhoto(Pageable pageable);

    long countLentsStartedAtBetween(LocalDateTime start, LocalDateTime end);

    long countLentsEndedAtBetween(LocalDateTime start, LocalDateTime end);

    LentHistory save(LentHistory lentHistory);
}
