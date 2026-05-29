package com.gyeongsan.cabinet.adapter.in.web.watermelon.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BuyItemResponse {
    private final long seedBalance;
    private final int dropProtectionCount;
    private final int destroyProtectionCount;
    private final int premiumFertilizerCount;
    private final int dangerousFertilizerCount;
}
