package com.gyeongsan.cabinet.cabinet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildingStatusDto {
    private Long totalCounts;
    private Long totalAvailable;
    private Long totalFull;
    private Long totalBroken;
}
