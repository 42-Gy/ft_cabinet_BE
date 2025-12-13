package com.gyeongsan.cabinet.cabinet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CabinetStatusDto {

    private String section;
    private Long total;
    private Long availableCount;
    private Long fullCount;
    private Long brokenCount;
}
