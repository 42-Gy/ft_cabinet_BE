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

    /**
     * 만료일까지 남은 일수(달력 기준, 사물함 목록·/me·반납 응답과 같은 계산). 0은 만료일 당일, 음수는 만료일이 지난 일수. 대여 중이 아니면 null(목록은 0
     * 으로 채우지만 0 은 "오늘 만료"와 구분되지 않아 여기서는 null 로 둔다).
     */
    private Long daysRemaining;

    private String previousUserName;
    private LocalDateTime previousEndedAt;

    private Boolean isReservedByMe;
}
