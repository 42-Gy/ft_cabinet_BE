package com.gyeongsan.cabinet.adapter.in.web.watermelon.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class WatermelonStatusResponse {
    private final Long userId;
    private final String username;
    private final int currentLevel;
    private final int highestLevel;
    private final LocalDateTime highestLevelAchievedAt;
    private final long seedBalance;
    private final long rank;

    private final int totalAttempts;
    private final int totalSuccesses;
    private final int totalMaintains;
    private final int totalDrops;
    private final int totalDestroys;

    private final int dropProtectionCount;
    private final int destroyProtectionCount;
    private final int premiumFertilizerCount;
    private final int dangerousFertilizerCount;
}
