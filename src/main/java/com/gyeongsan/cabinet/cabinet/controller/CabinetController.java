package com.gyeongsan.cabinet.cabinet.controller;

import com.gyeongsan.cabinet.cabinet.dto.BuildingStatusDto;
import com.gyeongsan.cabinet.cabinet.dto.CabinetDetailResponseDto;
import com.gyeongsan.cabinet.cabinet.dto.CabinetListResponseDto;
import com.gyeongsan.cabinet.cabinet.dto.CabinetStatusDto;
import com.gyeongsan.cabinet.cabinet.service.CabinetService;
import com.gyeongsan.cabinet.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v4/cabinets")
@RequiredArgsConstructor
public class CabinetController {

    private final CabinetService cabinetService;

    @GetMapping
    public ApiResponse<List<CabinetListResponseDto>> getCabinetList(@RequestParam Integer floor) {
        List<CabinetListResponseDto> cabinetList = cabinetService.getCabinetList(floor);
        return ApiResponse.success(cabinetList);
    }

    @GetMapping("/status-summary")
    public ApiResponse<List<CabinetStatusDto>> getCabinetStatusSummary(@RequestParam Integer floor) {
        List<CabinetStatusDto> summary = cabinetService.getStatusSummaryByFloor(floor);
        return ApiResponse.success(summary);
    }

    @GetMapping("/status-summary/all")
    public ApiResponse<BuildingStatusDto> getBuildingStatus() {
        return ApiResponse.success(cabinetService.getBuildingStatus());
    }

    @GetMapping("/{cabinetId}")
    public ApiResponse<CabinetDetailResponseDto> getCabinetDetail(@PathVariable Long cabinetId) {
        return ApiResponse.success(cabinetService.getCabinetDetail(cabinetId));
    }
}
