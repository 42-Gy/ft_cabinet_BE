package com.gyeongsan.cabinet.adapter.in.web.cabinet.dto;

import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

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

    private Boolean isReservedByMe;
}
