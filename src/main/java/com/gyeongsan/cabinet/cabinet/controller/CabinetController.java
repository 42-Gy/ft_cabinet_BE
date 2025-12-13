package com.gyeongsan.cabinet.cabinet.controller;

import com.gyeongsan.cabinet.cabinet.dto.CabinetListResponseDto;
import com.gyeongsan.cabinet.cabinet.dto.CabinetStatusDto;
import com.gyeongsan.cabinet.cabinet.service.CabinetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // 👈 필요시 추가
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v4/cabinets")
@RequiredArgsConstructor
public class CabinetController {

    private final CabinetService cabinetService;

    /**
     * [1] 사물함 목록 조회 (개별 정보)
     * GET /v4/cabinets?floor=2
     */
    @GetMapping("")
    public ResponseEntity<List<CabinetListResponseDto>> getCabinetList(@RequestParam Integer floor) {
        List<CabinetListResponseDto> cabinetList = cabinetService.getCabinetList(floor);
        return ResponseEntity.ok(cabinetList);
    }

    /**
     * [2] 사물함 현황 통계 조회 (섹션별 개수)
     * GET /v4/cabinets/status-summary?floor=2
     */
    @GetMapping("/status-summary")
    public ResponseEntity<List<CabinetStatusDto>> getCabinetStatusSummary(@RequestParam Integer floor) {
        List<CabinetStatusDto> summary = cabinetService.getStatusSummaryByFloor(floor);
        return ResponseEntity.ok(summary);
    }
}