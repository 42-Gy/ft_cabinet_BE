package com.gyeongsan.cabinet.domain.watermelon.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class WatermelonEventLog {
    private final Long id;
    private final Long userId;
    private final int beforeLevel;
    private final int afterLevel;
    private final boolean usedPremiumFertilizer;
    private final boolean usedDangerousFertilizer;
    private final boolean usedDropProtection;
    private final boolean usedDestroyProtection;
    private final EnhancementResult rawOutcome;
    private final EnhancementResult finalOutcome;
    private final int costSeeds;
    private final LocalDateTime createdAt;
}
