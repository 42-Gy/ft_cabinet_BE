package com.gyeongsan.cabinet.cabinet.dto;

import com.gyeongsan.cabinet.cabinet.domain.LentType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CabinetPendingResponseDto {
    private Integer visibleNum;
    private String statusNote;
    private LentType lentType;
    private String photoUrl;
    private String intraId;
}
