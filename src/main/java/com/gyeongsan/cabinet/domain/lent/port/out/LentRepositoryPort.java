package com.gyeongsan.cabinet.domain.lent.port.out;

import com.gyeongsan.cabinet.domain.lent.model.LentHistory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LentRepositoryPort {

    Optional<LentHistory> findByUserIdAndEndedAtIsNull(Long userId);

    Optional<LentHistory> findByCabinetIdAndEndedAtIsNull(Long cabinetId);

    Optional<LentHistory> findTopByCabinetIdAndEndedAtIsNotNullOrderByEndedAtDesc(Long cabinetId);

    long countByEndedAtIsNull();

    List<LentHistory> findAllOverdueLentHistories(LocalDateTime now);

    List<LentHistory> findAllActiveLentByCabinetIds(List<Long> cabinetIds);

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
