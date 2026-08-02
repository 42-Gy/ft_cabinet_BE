package com.gyeongsan.cabinet.domain.cabinet.port.in;

import com.gyeongsan.cabinet.adapter.in.web.cabinet.dto.BuildingStatusDto;
import com.gyeongsan.cabinet.adapter.in.web.cabinet.dto.CabinetDetailResponseDto;
import com.gyeongsan.cabinet.adapter.in.web.cabinet.dto.CabinetListResponseDto;
import com.gyeongsan.cabinet.adapter.in.web.cabinet.dto.CabinetStatusDto;
import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import java.util.List;

public interface CabinetQueryUseCase {

    List<CabinetListResponseDto> getCabinetList(Integer floor, UserPrincipal userPrincipal);

    List<CabinetStatusDto> getStatusSummaryByFloor(Integer floor);

    BuildingStatusDto getBuildingStatus();

    CabinetDetailResponseDto getCabinetDetail(Long cabinetId, UserPrincipal userPrincipal);
}
