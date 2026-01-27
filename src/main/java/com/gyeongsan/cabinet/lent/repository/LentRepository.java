package com.gyeongsan.cabinet.lent.repository;

import com.gyeongsan.cabinet.lent.domain.LentHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LentRepository extends JpaRepository<LentHistory, Long> {

        Optional<LentHistory> findByUserIdAndEndedAtIsNull(Long userId);

        Optional<LentHistory> findByCabinetIdAndEndedAtIsNull(Long cabinetId);

        long countByEndedAtIsNull();

        @Query("SELECT lh FROM LentHistory lh JOIN FETCH lh.cabinet JOIN FETCH lh.user " +
                        "WHERE lh.endedAt IS NULL AND lh.expiredAt < :now")
        List<LentHistory> findAllOverdueLentHistories(@Param("now") LocalDateTime now);

        @Query("SELECT lh FROM LentHistory lh JOIN FETCH lh.user JOIN FETCH lh.cabinet c WHERE c.id IN :cabinetIds AND lh.endedAt IS NULL")
        List<LentHistory> findAllActiveLentByCabinetIds(@Param("cabinetIds") List<Long> cabinetIds);

        Optional<LentHistory> findTopByCabinetIdAndEndedAtIsNotNullOrderByEndedAtDesc(Long cabinetId);

        @Query("SELECT lh FROM LentHistory lh " +
                        "JOIN FETCH lh.user u " +
                        "JOIN lh.cabinet c " +
                        "WHERE c.visibleNum = :visibleNum " +
                        "ORDER BY lh.startedAt DESC")
        Page<LentHistory> findHistoryByCabinet(@Param("visibleNum") Integer visibleNum, Pageable pageable);

        @Query("SELECT lh FROM LentHistory lh " +
                        "JOIN FETCH lh.user " +
                        "JOIN FETCH lh.cabinet " +
                        "WHERE lh.expiredAt BETWEEN :start AND :end " +
                        "AND lh.endedAt IS NULL")
        List<LentHistory> findAllActiveLentsByExpiredAtBetween(
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("SELECT lh FROM LentHistory lh JOIN FETCH lh.user WHERE lh.endedAt IS NULL")
        List<LentHistory> findAllActiveLents();

        @Query("SELECT count(lh) FROM LentHistory lh WHERE lh.startedAt BETWEEN :start AND :end")
        long countLentsStartedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        @Query("SELECT count(lh) FROM LentHistory lh WHERE lh.endedAt BETWEEN :start AND :end")
        long countLentsEndedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        @Query("SELECT lh FROM LentHistory lh " +
                        "JOIN FETCH lh.cabinet c " +
                        "JOIN FETCH lh.user u " +
                        "WHERE c.status = 'PENDING' " +
                        "AND lh.endedAt = (SELECT MAX(lh2.endedAt) FROM LentHistory lh2 WHERE lh2.cabinet = c)")
        List<LentHistory> findAllLatestLentForPendingCabinets();

        @Query("SELECT lh FROM LentHistory lh " +
                        "JOIN FETCH lh.user " +
                        "JOIN FETCH lh.cabinet " +
                        "WHERE lh.endedAt IS NULL " +
                        "AND lh.expiredAt BETWEEN :start AND :end")
        List<LentHistory> findRecentExpiredActiveLents(
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);
}
