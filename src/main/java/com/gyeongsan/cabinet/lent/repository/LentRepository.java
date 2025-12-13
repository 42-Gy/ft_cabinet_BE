package com.gyeongsan.cabinet.lent.repository;

import com.gyeongsan.cabinet.lent.domain.LentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LentRepository extends JpaRepository<LentHistory, Long> {

    Optional<LentHistory> findByUserIdAndEndedAtIsNull(Long userId);

    Optional<LentHistory> findByCabinetIdAndEndedAtIsNull(Long cabinetId);

    @Query("SELECT lh FROM LentHistory lh JOIN FETCH lh.cabinet JOIN FETCH lh.user " +
            "WHERE lh.endedAt IS NULL AND lh.expiredAt < :now")
    List<LentHistory> findAllOverdueLentHistories(@Param("now") LocalDateTime now);

    @Query("SELECT lh FROM LentHistory lh JOIN FETCH lh.user JOIN FETCH lh.cabinet c WHERE c.id IN :cabinetIds AND lh.endedAt IS NULL")
    List<LentHistory> findAllActiveLentByCabinetIds(@Param("cabinetIds") List<Long> cabinetIds);

    Optional<LentHistory> findTopByCabinetIdAndEndedAtIsNotNullOrderByEndedAtDesc(Long cabinetId);
}
