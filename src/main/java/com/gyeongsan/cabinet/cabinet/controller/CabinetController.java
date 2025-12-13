package com.gyeongsan.cabinet.cabinet.controller;

import com.gyeongsan.cabinet.cabinet.dto.BuildingStatusDto;
import com.gyeongsan.cabinet.cabinet.dto.CabinetDetailResponseDto;
import com.gyeongsan.cabinet.cabinet.dto.CabinetListResponseDto;
import com.gyeongsan.cabinet.cabinet.dto.CabinetStatusDto;
import com.gyeongsan.cabinet.cabinet.service.CabinetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v4/cabinets")
@RequiredArgsConstructor
public class CabinetController {

    private final CabinetService cabinetService;

    @GetMapping
    public ResponseEntity<List<CabinetListResponseDto>> getCabinetList(@RequestParam Integer floor) {
        List<CabinetListResponseDto> cabinetList = cabinetService.getCabinetList(floor);
        return ResponseEntity.ok(cabinetList);
    }

    @GetMapping("/status-summary")
    public ResponseEntity<List<CabinetStatusDto>> getCabinetStatusSummary(@RequestParam Integer floor) {
        List<CabinetStatusDto> summary = cabinetService.getStatusSummaryByFloor(floor);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/status-summary/all")
    public ResponseEntity<BuildingStatusDto> getBuildingStatus() {
        return ResponseEntity.ok(cabinetService.getBuildingStatus());
    }

    @GetMapping("/{cabinetId}")
    public ResponseEntity<CabinetDetailResponseDto> getCabinetDetail(@PathVariable Long cabinetId) {
        return ResponseEntity.ok(cabinetService.getCabinetDetail(cabinetId));
    }
}
