package com.gyeongsan.cabinet.adapter.in.web.watermelon.dto;

import com.gyeongsan.cabinet.domain.watermelon.domain.EnhancementResult;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EnhanceResponse {
    private final int beforeLevel;
    private final int afterLevel;
    private final EnhancementResult rawOutcome;
    private final EnhancementResult finalOutcome;
    private final long seedBalance;
    private final int dropProtectionCount;
    private final int destroyProtectionCount;
    private final int premiumFertilizerCount;
    private final int dangerousFertilizerCount;
}
