package com.gyeongsan.cabinet.domain.watermelon.port.in;

import com.gyeongsan.cabinet.domain.watermelon.domain.Watermelon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GetWatermelonLeaderboardUseCase {
    Page<Watermelon> getLeaderboard(Pageable pageable);
}
