package com.gyeongsan.cabinet.adapter.out.persistence.coin;

import com.gyeongsan.cabinet.coin.domain.CoinHistory;
import com.gyeongsan.cabinet.coin.domain.CoinLogType;
import com.gyeongsan.cabinet.coin.repository.CoinHistoryRepository;
import com.gyeongsan.cabinet.domain.coin.port.out.CoinHistoryRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CoinHistoryPersistenceAdapter implements CoinHistoryRepositoryPort {

    private final CoinHistoryRepository coinHistoryRepository;

    @Override
    public CoinHistory save(CoinHistory coinHistory) {
        return coinHistoryRepository.save(coinHistory);
    }

    @Override
    public List<CoinHistory> findAllByUserIdOrderByCreatedAtDesc(Long userId) {
        return coinHistoryRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<Object[]> sumIssuedAndUsedBetween(LocalDateTime start, LocalDateTime end) {
        return coinHistoryRepository.sumIssuedAndUsedBetween(start, end);
    }

    @Override
    public long countByTypeAndCreatedAtBetween(CoinLogType type, LocalDateTime start, LocalDateTime end) {
        return coinHistoryRepository.countByTypeAndCreatedAtBetween(type, start, end);
    }
}
