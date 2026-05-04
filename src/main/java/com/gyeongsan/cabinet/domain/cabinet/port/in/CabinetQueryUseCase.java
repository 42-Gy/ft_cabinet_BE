package com.gyeongsan.cabinet.domain.cabinet.port.in;

import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import com.gyeongsan.cabinet.cabinet.dto.BuildingStatusDto;
import com.gyeongsan.cabinet.cabinet.dto.CabinetDetailResponseDto;
import com.gyeongsan.cabinet.cabinet.dto.CabinetListResponseDto;
import com.gyeongsan.cabinet.cabinet.dto.CabinetStatusDto;

import java.util.List;

public interface CabinetQueryUseCase {

    List<CabinetListResponseDto> getCabinetList(Integer floor, UserPrincipal userPrincipal);

    List<CabinetStatusDto> getStatusSummaryByFloor(Integer floor);

    BuildingStatusDto getBuildingStatus();

    CabinetDetailResponseDto getCabinetDetail(Long cabinetId, UserPrincipal userPrincipal);
}
