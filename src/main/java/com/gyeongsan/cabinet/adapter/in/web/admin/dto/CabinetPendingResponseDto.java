package com.gyeongsan.cabinet.adapter.in.web.admin.dto;

import com.gyeongsan.cabinet.domain.cabinet.model.LentType;
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
