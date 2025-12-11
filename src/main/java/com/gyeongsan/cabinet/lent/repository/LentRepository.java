package com.gyeongsan.cabinet.lent.repository;

import com.gyeongsan.cabinet.lent.domain.LentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LentRepository extends JpaRepository<LentHistory, Long> {

    // 1. [기존] 유저가 대여 중인 기록 찾기
    Optional<LentHistory> findByUserIdAndEndedAtIsNull(Long userId);

    // 2. [추가] 사물함이 대여 중인지 확인 (LentFacadeService에서 쓸 수도 있음)
    Optional<LentHistory> findByCabinetIdAndEndedAtIsNull(Long cabinetId);

    // 3. [추가] 연체자 단속용 쿼리 (스케줄러가 사용)
    // 반납 안 함(endedAt is null) + 반납 기한 지남(expiredAt < now)
    @Query("SELECT lh FROM LentHistory lh JOIN FETCH lh.cabinet " +
            "WHERE lh.endedAt IS NULL AND lh.expiredAt < :now")
    List<LentHistory> findAllOverdueLentHistories(@Param("now") LocalDateTime now);
}