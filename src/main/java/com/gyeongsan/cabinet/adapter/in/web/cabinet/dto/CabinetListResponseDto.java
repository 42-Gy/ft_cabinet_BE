package com.gyeongsan.cabinet.adapter.in.web.cabinet.dto;

import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
