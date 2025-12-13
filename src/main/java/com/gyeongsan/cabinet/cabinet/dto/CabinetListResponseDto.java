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
    private String lentType;
    private CabinetStatus status;
    private String statusNote;

    private String lentUserName;
    private LocalDateTime lentStartedAt;
    private LocalDateTime lentExpiredAt;
    private Long daysRemaining;
}
