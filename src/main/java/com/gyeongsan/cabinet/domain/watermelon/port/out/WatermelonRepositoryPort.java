package com.gyeongsan.cabinet.domain.watermelon.port.out;

import com.gyeongsan.cabinet.domain.watermelon.domain.Watermelon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface WatermelonRepositoryPort {
    Optional<Watermelon> findByUserId(Long userId);
    Watermelon save(Watermelon watermelon);
    Page<Watermelon> findAllOrderByRanking(Pageable pageable);
    long findRankByUserId(Long userId);
}
