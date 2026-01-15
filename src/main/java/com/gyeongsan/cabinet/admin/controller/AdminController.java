package com.gyeongsan.cabinet.admin.controller;

import com.gyeongsan.cabinet.admin.dto.*;
import com.gyeongsan.cabinet.admin.service.AdminService;
import com.gyeongsan.cabinet.cabinet.dto.CabinetPendingResponseDto;
import com.gyeongsan.cabinet.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.List;

@RestController
@RequestMapping("/v4/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardResponse> getDashboard() {
        return ApiResponse.success(adminService.getDashboard());
    }

    @GetMapping("/users/{name}")
    public ApiResponse<AdminUserDetailResponse> searchUser(@PathVariable String name) {
        return ApiResponse.success(adminService.getUserDetail(name));
    }

    @PostMapping("/users/{name}/coin")
    public ApiResponse<String> provideCoin(
            @PathVariable String name,
            @RequestBody CoinProvideRequest request) {
        adminService.provideCoin(name, request);
        return ApiResponse.success("코인 지급 완료");
    }

    @PatchMapping("/users/{name}/logtime")
    public ApiResponse<String> updateUserLogtime(
            @PathVariable String name,
            @RequestBody UserLogtimeRequest request) {
        Integer monthlyLogtime = request.monthlyLogtime();

        if (monthlyLogtime == null || monthlyLogtime < 0) {
            return ApiResponse.fail(400, "유효하지 않은 시간 값입니다.");
        }

        adminService.updateUserLogtime(name, monthlyLogtime);
        return ApiResponse.success("로그타임 수정 완료");
    }

    @PatchMapping("/cabinets/{visibleNum}")
    public ApiResponse<String> updateCabinet(
            @PathVariable Integer visibleNum,
            @RequestBody CabinetStatusRequest request) {
        adminService.updateCabinetStatus(visibleNum, request);
        return ApiResponse.success("사물함 상태 변경 완료");
    }

    @PostMapping("/cabinets/{visibleNum}/force-return")
    public ApiResponse<String> forceReturn(@PathVariable Integer visibleNum) {
        adminService.forceReturn(visibleNum);
        return ApiResponse.success("강제 반납 완료 (상태: 수동 확인 대기(PENDING)로 변경됨)");
    }

    @GetMapping("/cabinets/pending")
    public ApiResponse<List<CabinetPendingResponseDto>> getPendingCabinets() {
        return ApiResponse.success(adminService.getPendingCabinets());
    }

    @PostMapping("/cabinets/{visibleNum}/approve")
    public ApiResponse<String> approveManualReturn(@PathVariable Integer visibleNum) {
        adminService.approveManualReturn(visibleNum);
        return ApiResponse.success("수동 반납 승인 완료! (사물함이 사용 가능 상태로 변경되었습니다)");
    }

    @PatchMapping("/items/{itemName}/price")
    public ApiResponse<String> updateItemPrice(
            @PathVariable String itemName,
            @RequestBody ItemPriceRequest request) {
        Long newPrice = request.price();

        if (newPrice == null) {
            return ApiResponse.fail(400, "가격(price) 정보가 필요합니다.");
        }

        adminService.updateItemPrice(itemName, newPrice);
        return ApiResponse.success("아이템 가격이 변경되었습니다.");
    }

    @GetMapping("/cabinets/overdue")
    public ApiResponse<List<OverdueUserResponse>> getOverdueUsers() {
        return ApiResponse.success(adminService.getOverdueUsers());
    }

    @GetMapping("/cabinets/{visibleNum}")
    public ApiResponse<CabinetDetailResponse> getCabinetDetail(@PathVariable Integer visibleNum) {
        return ApiResponse.success(adminService.getCabinetDetail(visibleNum));
    }

    public record UserLogtimeRequest(Integer monthlyLogtime) {
    }

    public record ItemPriceRequest(Long price) {
    }
}
