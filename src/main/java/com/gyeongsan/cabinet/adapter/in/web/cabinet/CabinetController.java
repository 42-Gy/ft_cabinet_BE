package com.gyeongsan.cabinet.adapter.in.web.cabinet;

import com.gyeongsan.cabinet.adapter.in.web.cabinet.dto.BuildingStatusDto;
import com.gyeongsan.cabinet.adapter.in.web.cabinet.dto.CabinetDetailResponseDto;
import com.gyeongsan.cabinet.adapter.in.web.cabinet.dto.CabinetListResponseDto;
import com.gyeongsan.cabinet.adapter.in.web.cabinet.dto.CabinetStatusDto;
import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import com.gyeongsan.cabinet.common.ApiResponse;
import com.gyeongsan.cabinet.domain.cabinet.port.in.CabinetQueryUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v4/cabinets")
@RequiredArgsConstructor
public class CabinetController {

    private final CabinetQueryUseCase cabinetQueryUseCase;

    @GetMapping
    public ApiResponse<List<CabinetListResponseDto>> getCabinetList(
            @RequestParam Integer floor, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<CabinetListResponseDto> cabinetList =
                cabinetQueryUseCase.getCabinetList(floor, userPrincipal);
        return ApiResponse.success(cabinetList);
    }

    @GetMapping("/status-summary")
    public ApiResponse<List<CabinetStatusDto>> getCabinetStatusSummary(
            @RequestParam Integer floor) {
        List<CabinetStatusDto> summary = cabinetQueryUseCase.getStatusSummaryByFloor(floor);
        return ApiResponse.success(summary);
    }

    @GetMapping("/status-summary/all")
    public ApiResponse<BuildingStatusDto> getBuildingStatus() {
        return ApiResponse.success(cabinetQueryUseCase.getBuildingStatus());
    }

    @GetMapping("/{cabinetId}")
    public ApiResponse<CabinetDetailResponseDto> getCabinetDetail(
            @PathVariable Long cabinetId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(cabinetQueryUseCase.getCabinetDetail(cabinetId, userPrincipal));
    }
}
