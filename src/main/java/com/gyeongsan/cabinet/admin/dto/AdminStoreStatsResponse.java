package com.gyeongsan.cabinet.admin.dto;

import java.util.Map;

public record AdminStoreStatsResponse(
        long totalUserCoins,
        long totalUsedCoins,
        Map<String, Long> itemSales) {
}
