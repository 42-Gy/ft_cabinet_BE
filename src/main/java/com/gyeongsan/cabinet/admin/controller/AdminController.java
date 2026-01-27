package com.gyeongsan.cabinet.admin.controller;

import com.gyeongsan.cabinet.admin.dto.*;
import com.gyeongsan.cabinet.admin.service.AdminService;

import com.gyeongsan.cabinet.common.ApiResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;

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

    @PatchMapping("/cabinets/bundle/status")
    public ApiResponse<String> bulkUpdateCabinetStatus(@RequestBody BulkStatusUpdateRequest request) {
        adminService.bulkUpdateCabinetStatus(request);
        return ApiResponse.success("사물함 상태 일괄 변경 완료");
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

    @GetMapping("/stats/weekly")
    public ApiResponse<AdminWeeklyStatsResponse> getWeeklyStats() {
        return ApiResponse.success(adminService.getWeeklyStats());
    }

    @GetMapping("/stats/floors")
    public ApiResponse<AdminFloorStatsResponse> getFloorStats() {
        return ApiResponse.success(adminService.getFloorStats());
    }

    @GetMapping("/stats/coins")
    public ApiResponse<AdminCoinStatsResponse> getCoinStats() {
        return ApiResponse.success(adminService.getCoinStats());
    }

    @GetMapping("/stats/items")
    public ApiResponse<AdminItemUsageStatsResponse> getItemUsageStats() {
        return ApiResponse.success(adminService.getItemUsageStats());
    }

    @GetMapping("/users/penalty")
    public ApiResponse<List<PenaltyUserResponse>> getPenaltyUsers() {
        return ApiResponse.success(adminService.getPenaltyUsers());
    }

    @GetMapping("/cabinets/broken")
    public ApiResponse<List<BrokenCabinetResponse>> getBrokenCabinets() {
        return ApiResponse.success(adminService.getBrokenCabinets());
    }

    @GetMapping("/cabinets/{visibleNum}")
    public ApiResponse<CabinetDetailResponse> getCabinetDetail(@PathVariable Integer visibleNum) {
        return ApiResponse.success(adminService.getCabinetDetail(visibleNum));
    }

    @PostMapping("/users/{name}/penalty")
    public ApiResponse<String> givePenalty(
            @PathVariable String name,
            @RequestBody PenaltyRequest request) {
        adminService.givePenalty(name, request);
        return ApiResponse.success("패널티 부여 완료");
    }

    @DeleteMapping("/users/{name}/penalty")
    public ApiResponse<String> deletePenalty(@PathVariable String name) {
        adminService.deletePenalty(name);
        return ApiResponse.success("유저 패널티 해제 완료");
    }

    @PostMapping("/users/{name}/items")
    public ApiResponse<String> grantItem(
            @PathVariable String name,
            @RequestBody ItemGrantRequest request) {
        adminService.grantItem(name, request);
        return ApiResponse.success("아이템 지급 완료");
    }

    @PostMapping("/alarm/emergency")
    public ApiResponse<String> sendEmergencyNotice(@RequestBody EmergencyNoticeRequest request) {
        adminService.sendEmergencyNotice(request.message());
        return ApiResponse.success("긴급 공지 발송 완료 (현재 대여중인 유저 대상)");
    }

    @GetMapping("/cabinets/{visibleNum}/history")
    public ApiResponse<Page<CabinetHistoryResponse>> getCabinetHistory(
            @PathVariable Integer visibleNum,
            Pageable pageable) {
        return ApiResponse.success(adminService.getCabinetHistory(visibleNum, pageable));
    }

    @GetMapping("/stats/store")
    public ApiResponse<AdminStoreStatsResponse> getStoreStats() {
        return ApiResponse.success(adminService.getStoreStats());
    }

    @GetMapping("/stats/attendance")
    public ApiResponse<List<AttendanceStatResponse>> getAttendanceStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(adminService.getAttendanceStats(startDate, endDate));
    }

    @PostMapping("/users/{name}/role/admin")
    public ApiResponse<String> promoteUserToAdmin(@PathVariable String name) {
        adminService.promoteUserToAdmin(name);
        return ApiResponse.success("관리자 권한 부여 완료");
    }

    @DeleteMapping("/users/{name}/role/admin")
    public ApiResponse<String> demoteUserToUser(@PathVariable String name) {
        adminService.demoteUserToUser(name);
        return ApiResponse.success("관리자 권한 해제 완료");
    }

    @DeleteMapping("/users/{name}/items")
    public ApiResponse<String> revokeUserItem(
            @PathVariable String name,
            @RequestBody ItemRevokeRequest request) {
        adminService.revokeUserItem(name, request);
        return ApiResponse.success("아이템 회수 완료 (미사용 아이템 전체 삭제)");
    }

    @DeleteMapping("/users/{name}/coin")
    public ApiResponse<String> revokeUserCoin(
            @PathVariable String name,
            @RequestBody CoinRevokeRequest request) {
        adminService.revokeUserCoin(name, request);
        return ApiResponse.success("코인 회수 완료");
    }

    public record UserLogtimeRequest(Integer monthlyLogtime) {
    }

    public record ItemPriceRequest(Long price) {
    }
}
