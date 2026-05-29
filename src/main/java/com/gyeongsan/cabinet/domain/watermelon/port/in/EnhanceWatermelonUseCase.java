package com.gyeongsan.cabinet.domain.watermelon.port.in;

import com.gyeongsan.cabinet.domain.watermelon.domain.WatermelonEnhanceResult;

public interface EnhanceWatermelonUseCase {
    WatermelonEnhanceResult enhance(Long userId, boolean usePremium, boolean useDangerous, boolean useDropProj, boolean useDestroyProj);
}
