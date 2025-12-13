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
    private String section;             // 섹션 이름 (예: Section 1)
    private Long total;                 // 총 사물함 개수
    private Long availableCount;        // 사용 가능 (AVAILABLE) 개수
    private Long fullCount;             // 사용 중 (FULL + OVERDUE) 개수
    private Long brokenCount;           // 점검 중 (BROKEN + DISABLED) 개수
}