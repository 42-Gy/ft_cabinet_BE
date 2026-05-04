package com.gyeongsan.cabinet.domain.coin.port.out;

import com.gyeongsan.cabinet.coin.domain.CoinHistory;
import com.gyeongsan.cabinet.coin.domain.CoinLogType;

import java.time.LocalDateTime;
import java.util.List;

public interface CoinHistoryRepositoryPort {

    CoinHistory save(CoinHistory coinHistory);

    List<CoinHistory> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<Object[]> sumIssuedAndUsedBetween(LocalDateTime start, LocalDateTime end);

    long countByTypeAndCreatedAtBetween(CoinLogType type, LocalDateTime start, LocalDateTime end);
}
