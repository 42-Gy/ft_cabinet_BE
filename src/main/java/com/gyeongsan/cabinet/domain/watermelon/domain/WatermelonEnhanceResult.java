package com.gyeongsan.cabinet.domain.watermelon.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WatermelonEnhanceResult {
    private final int beforeLevel;
    private final int afterLevel;
    private final EnhancementResult rawOutcome;
    private final EnhancementResult finalOutcome;
    private final Watermelon watermelon;
}
