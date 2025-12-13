package com.gyeongsan.cabinet.cabinet.dto;

import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CabinetDetailResponseDto {
    private Long cabinetId;
    private Integer visibleNum;
    private Integer floor;
    private String section;
    private CabinetStatus status;
    private String statusNote;

    private String lentUserName;
    private LocalDateTime lentStartedAt;
    private LocalDateTime lentExpiredAt;

    private String previousUserName;
    private LocalDateTime previousEndedAt;
}
