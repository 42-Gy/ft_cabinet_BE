package com.gyeongsan.cabinet.domain.watermelon.port.in;

import com.gyeongsan.cabinet.domain.watermelon.domain.Watermelon;

public interface GetWatermelonStatusUseCase {
    Watermelon getStatus(Long userId);
}
