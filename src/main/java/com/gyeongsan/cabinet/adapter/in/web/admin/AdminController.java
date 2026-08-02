package com.gyeongsan.cabinet.adapter.in.web.admin;

import com.gyeongsan.cabinet.adapter.in.web.admin.dto.*;
import com.gyeongsan.cabinet.common.ApiResponse;
import com.gyeongsan.cabinet.domain.admin.port.in.AdminAlarmUseCase;
import com.gyeongsan.cabinet.domain.admin.port.in.AdminBannedUserUseCase;
import com.gyeongsan.cabinet.domain.admin.port.in.AdminCabinetUseCase;
import com.gyeongsan.cabinet.domain.admin.port.in.AdminDashboardUseCase;
import com.gyeongsan.cabinet.domain.admin.port.in.AdminItemCoinUseCase;
import com.gyeongsan.cabinet.domain.admin.port.in.AdminUserUseCase;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v4/admin")
@RequiredArgsConstructor
@RateLimiter(name = "userApi")
public class AdminController {

    private final AdminDashboardUseCase adminDashboardUseCase;
    private final AdminUserUseCase adminUserUseCase;
    private final AdminCabinetUseCase adminCabinetUseCase;
    private final AdminItemCoinUseCase adminItemCoinUseCase;
    private final AdminBannedUserUseCase adminBannedUserUseCase;
    private final AdminAlarmUseCase adminAlarmUseCase;

    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardResponse> getDashboard() {
        return ApiResponse.success(adminDashboardUseCase.getDashboard());
    }

    @GetMapping("/users")
    public ApiResponse<Page<AdminAllUsersResponseDto>> getAllUsers(Pageable pageable) {
        return ApiResponse.success(adminUserUseCase.getAllUsers(pageable));
    }

    @GetMapping("/users/{name}")
    public ApiResponse<AdminUserDetailResponse> searchUser(@PathVariable String name) {
        return ApiResponse.success(adminUserUseCase.getUserDetail(name));
    }

    @PostMapping("/users/{name}/coin")
    public ApiResponse<String> provideCoin(
            @PathVariable String name, @RequestBody CoinProvideRequest request) {
        adminItemCoinUseCase.provideCoin(name, request);
        return ApiResponse.success("씨앗 지급 완료");
    }

    @PatchMapping("/users/{name}/logtime")
    public ApiResponse<String> updateUserLogtime(
            @PathVariable String name, @RequestBody UserLogtimeRequest request) {
        Integer monthlyLogtime = request.monthlyLogtime();

        if (monthlyLogtime == null || monthlyLogtime < 0) {
            return ApiResponse.fail(400, "유효하지 않은 시간 값입니다.");
        }

        adminUserUseCase.updateUserLogtime(name, monthlyLogtime);
        return ApiResponse.success("로그타임 수정 완료");
    }

    @PatchMapping("/cabinets/{visibleNum}")
    public ApiResponse<String> updateCabinet(
            @PathVariable Integer visibleNum, @RequestBody CabinetStatusRequest request) {
        adminCabinetUseCase.updateCabinetStatus(visibleNum, request);
        return ApiResponse.success("사물함 상태 변경 완료");
    }

    @PatchMapping("/cabinets/bundle/status")
    public ApiResponse<String> bulkUpdateCabinetStatus(
            @RequestBody BulkStatusUpdateRequest request) {
        adminCabinetUseCase.bulkUpdateCabinetStatus(request);
        return ApiResponse.success("사물함 상태 일괄 변경 완료");
    }

    @PostMapping("/cabinets/{visibleNum}/force-return")
    public ApiResponse<String> forceReturn(@PathVariable Integer visibleNum) {
        adminCabinetUseCase.forceReturn(visibleNum);
        return ApiResponse.success("강제 반납 완료 (상태: 수동 확인 대기(PENDING)로 변경됨)");
    }

    @GetMapping("/cabinets/pending")
    public ApiResponse<List<CabinetPendingResponseDto>> getPendingCabinets() {
        return ApiResponse.success(adminCabinetUseCase.getPendingCabinets());
    }

    @GetMapping("/returns/photos")
    public ApiResponse<Page<ReturnPhotoResponseDto>> getReturnPhotos(Pageable pageable) {
        return ApiResponse.success(adminCabinetUseCase.getReturnPhotos(pageable));
    }

    @PostMapping("/cabinets/{visibleNum}/approve")
    public ApiResponse<String> approveManualReturn(@PathVariable Integer visibleNum) {
        adminCabinetUseCase.approveManualReturn(visibleNum);
        return ApiResponse.success("수동 반납 승인 완료! (사물함이 사용 가능 상태로 변경되었습니다)");
    }

    @PatchMapping("/items/{itemName}/price")
    public ApiResponse<String> updateItemPrice(
            @PathVariable String itemName, @RequestBody ItemPriceRequest request) {
        Long newPrice = request.price();

        if (newPrice == null) {
            return ApiResponse.fail(400, "가격(price) 정보가 필요합니다.");
        }

        adminItemCoinUseCase.updateItemPrice(itemName, newPrice);
        return ApiResponse.success("아이템 가격이 변경되었습니다.");
    }

    @GetMapping("/cabinets/overdue")
    public ApiResponse<List<OverdueUserResponse>> getOverdueUsers() {
        return ApiResponse.success(adminUserUseCase.getOverdueUsers());
    }

    @GetMapping("/stats/weekly")
    public ApiResponse<AdminWeeklyStatsResponse> getWeeklyStats() {
        return ApiResponse.success(adminDashboardUseCase.getWeeklyStats());
    }

    @GetMapping("/stats/floors")
    public ApiResponse<AdminFloorStatsResponse> getFloorStats() {
        return ApiResponse.success(adminDashboardUseCase.getFloorStats());
    }

    @GetMapping("/stats/coins")
    public ApiResponse<AdminCoinStatsResponse> getCoinStats() {
        return ApiResponse.success(adminDashboardUseCase.getCoinStats());
    }

    @GetMapping("/stats/items")
    public ApiResponse<AdminItemUsageStatsResponse> getItemUsageStats() {
        return ApiResponse.success(adminDashboardUseCase.getItemUsageStats());
    }

    @GetMapping("/users/penalty")
    public ApiResponse<List<PenaltyUserResponse>> getPenaltyUsers() {
        return ApiResponse.success(adminUserUseCase.getPenaltyUsers());
    }

    @GetMapping("/cabinets/broken")
    public ApiResponse<List<BrokenCabinetResponse>> getBrokenCabinets() {
        return ApiResponse.success(adminCabinetUseCase.getBrokenCabinets());
    }

    @GetMapping("/cabinets/{visibleNum}")
    public ApiResponse<CabinetDetailResponse> getCabinetDetail(@PathVariable Integer visibleNum) {
        return ApiResponse.success(adminCabinetUseCase.getCabinetDetail(visibleNum));
    }

    @PostMapping("/users/{name}/penalty")
    public ApiResponse<String> givePenalty(
            @PathVariable String name, @RequestBody PenaltyRequest request) {
        adminUserUseCase.givePenalty(name, request);
        return ApiResponse.success("패널티 부여 완료");
    }

    @DeleteMapping("/users/{name}/penalty")
    public ApiResponse<String> deletePenalty(@PathVariable String name) {
        adminUserUseCase.deletePenalty(name);
        return ApiResponse.success("유저 패널티 해제 완료");
    }

    @PostMapping("/users/{name}/items")
    public ApiResponse<String> grantItem(
            @PathVariable String name, @RequestBody ItemGrantRequest request) {
        adminItemCoinUseCase.grantItem(name, request);
        return ApiResponse.success("아이템 지급 완료");
    }

    @PostMapping("/alarm/emergency")
    public ApiResponse<String> sendEmergencyNotice(@RequestBody EmergencyNoticeRequest request) {
        adminAlarmUseCase.sendEmergencyNotice(request.message());
        return ApiResponse.success("긴급 공지 발송 완료 (현재 대여중인 유저 대상)");
    }

    @GetMapping("/cabinets/{visibleNum}/history")
    public ApiResponse<Page<CabinetHistoryResponse>> getCabinetHistory(
            @PathVariable Integer visibleNum, Pageable pageable) {
        return ApiResponse.success(adminCabinetUseCase.getCabinetHistory(visibleNum, pageable));
    }

    @GetMapping("/stats/store")
    public ApiResponse<AdminStoreStatsResponse> getStoreStats() {
        return ApiResponse.success(adminDashboardUseCase.getStoreStats());
    }

    @GetMapping("/stats/attendance")
    public ApiResponse<List<AttendanceStatResponse>> getAttendanceStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate) {
        return ApiResponse.success(adminDashboardUseCase.getAttendanceStats(startDate, endDate));
    }

    @PostMapping("/users/{name}/role/admin")
    public ApiResponse<String> promoteUserToAdmin(@PathVariable String name) {
        adminUserUseCase.promoteUserToAdmin(name);
        return ApiResponse.success("관리자 권한 부여 완료");
    }

    @DeleteMapping("/users/{name}/role/admin")
    public ApiResponse<String> demoteUserToUser(@PathVariable String name) {
        adminUserUseCase.demoteUserToUser(name);
        return ApiResponse.success("관리자 권한 해제 완료");
    }

    @DeleteMapping("/users/{name}/items")
    public ApiResponse<String> revokeUserItem(
            @PathVariable String name, @RequestBody ItemRevokeRequest request) {
        adminItemCoinUseCase.revokeUserItem(name, request);
        return ApiResponse.success("아이템 회수 완료 (미사용 아이템 전체 삭제)");
    }

    @DeleteMapping("/users/{name}/coin")
    public ApiResponse<String> revokeUserCoin(
            @PathVariable String name, @RequestBody CoinRevokeRequest request) {
        adminItemCoinUseCase.revokeUserCoin(name, request);
        return ApiResponse.success("씨앗 회수 완료");
    }

    @GetMapping("/banned-users")
    public ApiResponse<List<BannedUserResponse>> getBannedUsers() {
        return ApiResponse.success(adminBannedUserUseCase.getBannedUsers());
    }

    @PostMapping("/banned-users")
    public ApiResponse<String> addBannedUser(@RequestBody BanUserRequest request) {
        adminBannedUserUseCase.addBannedUser(request.intraId(), request.reason());
        return ApiResponse.success("블랙리스트 등록 완료");
    }

    @DeleteMapping("/banned-users/{intraId}")
    public ApiResponse<String> removeBannedUser(@PathVariable String intraId) {
        adminBannedUserUseCase.removeBannedUser(intraId);
        return ApiResponse.success("블랙리스트 해제 완료");
    }

    public record UserLogtimeRequest(Integer monthlyLogtime) {}

    public record ItemPriceRequest(Long price) {}
}
