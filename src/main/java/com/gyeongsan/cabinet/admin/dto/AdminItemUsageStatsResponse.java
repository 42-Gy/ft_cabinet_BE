package com.gyeongsan.cabinet.admin.dto;

import java.util.List;

public record AdminItemUsageStatsResponse(
        List<ItemUsageStat> itemStats,
        long attendanceRewardsCount,
        long watermelonRewardsCount) {
    public record ItemUsageStat(
            String itemName,
            String itemType,
            long purchaseCount,
            long usedCount) {
    }
}
