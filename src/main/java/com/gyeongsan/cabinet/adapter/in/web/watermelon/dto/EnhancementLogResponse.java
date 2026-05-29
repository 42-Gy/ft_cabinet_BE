package com.gyeongsan.cabinet.adapter.in.web.watermelon.dto;

import com.gyeongsan.cabinet.domain.watermelon.domain.EnhancementResult;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EnhancementLogResponse {
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
