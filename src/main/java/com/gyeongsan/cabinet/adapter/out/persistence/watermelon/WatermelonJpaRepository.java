package com.gyeongsan.cabinet.adapter.out.persistence.watermelon;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface WatermelonJpaRepository extends JpaRepository<WatermelonEntity, Long> {

    Optional<WatermelonEntity> findByUserId(Long userId);

    @Query("SELECT w FROM WatermelonEntity w ORDER BY w.highestLevel DESC, w.highestLevelAchievedAt ASC, w.totalAttempts ASC")
    Page<WatermelonEntity> findAllOrderByRanking(Pageable pageable);

    @Query("SELECT COUNT(w) + 1 FROM WatermelonEntity w WHERE " +
           "w.highestLevel > :highestLevel OR " +
           "(w.highestLevel = :highestLevel AND w.highestLevelAchievedAt < :highestLevelAchievedAt) OR " +
           "(w.highestLevel = :highestLevel AND w.highestLevelAchievedAt = :highestLevelAchievedAt AND w.totalAttempts < :totalAttempts) OR " +
           "(w.highestLevel = :highestLevel AND w.highestLevelAchievedAt = :highestLevelAchievedAt AND w.totalAttempts = :totalAttempts AND w.userId < :userId)")
    long countBetterThan(@Param("highestLevel") int highestLevel,
                         @Param("highestLevelAchievedAt") LocalDateTime highestLevelAchievedAt,
                         @Param("totalAttempts") int totalAttempts,
                         @Param("userId") Long userId);
}
