package com.gyeongsan.cabinet.cabinet.dto;

import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CabinetListResponseDto {

    private Long cabinetId;
    private Integer visibleNum;
    private Integer floor;
    private String section;
    private String lentType; // PRIVATE, SHARE
    private CabinetStatus status;
    private String statusNote; // 고장 사유 등

    // 대여 정보 (사물함이 FULL, OVERDUE일 때 의미 있음)
    private String lentUserName;
    private LocalDateTime lentStartedAt;
    private LocalDateTime lentExpiredAt;
    private Long daysRemaining; // 만료일까지 남은 일수 (양수), 연체일 (음수)
}