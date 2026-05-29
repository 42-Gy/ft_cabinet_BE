package com.gyeongsan.cabinet.adapter.in.web.watermelon.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class WatermelonConfigResponse {
    private final int maxLevel;
    private final List<Integer> enhanceCosts;
    private final List<Double> successRates;
    private final List<Double> maintainRates;
    private final List<Double> dropRates;
    private final List<Double> destroyRates;
    private final Map<String, Integer> itemPrices;
}
