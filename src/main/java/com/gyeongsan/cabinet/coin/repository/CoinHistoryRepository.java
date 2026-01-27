package com.gyeongsan.cabinet.coin.repository;

import com.gyeongsan.cabinet.coin.domain.CoinHistory;
import com.gyeongsan.cabinet.coin.domain.CoinLogType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CoinHistoryRepository extends JpaRepository<CoinHistory, Long> {

        @Query("""
                        SELECT
                            SUM(CASE WHEN ch.amount > 0 THEN ch.amount ELSE 0 END),
                            SUM(CASE WHEN ch.amount < 0 THEN ABS(ch.amount) ELSE 0 END)
                        FROM CoinHistory ch
                        WHERE ch.createdAt BETWEEN :start AND :end
                        """)
        Object[] sumIssuedAndUsedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        @Query("""
                        SELECT COUNT(ch)
                        FROM CoinHistory ch
                        WHERE ch.type = :type
                        AND ch.createdAt BETWEEN :start AND :end
                        """)
        long countByTypeAndCreatedAtBetween(
                        @Param("type") CoinLogType type,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        List<CoinHistory> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
