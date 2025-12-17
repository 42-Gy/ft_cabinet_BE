package com.gyeongsan.cabinet.admin.controller;

import com.gyeongsan.cabinet.admin.dto.*;
import com.gyeongsan.cabinet.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v4/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboard());
    }

    @GetMapping("/users/{name}")
    public ResponseEntity<AdminUserDetailResponse> searchUser(@PathVariable String name) {
        return ResponseEntity.ok(adminService.getUserDetail(name));
    }

    @PostMapping("/users/{name}/coin")
    public ResponseEntity<String> provideCoin(
            @PathVariable String name,
            @RequestBody CoinProvideRequest request
    ) {
        adminService.provideCoin(name, request);
        return ResponseEntity.ok("코인 지급 완료");
    }

    @PatchMapping("/users/{name}/logtime")
    public ResponseEntity<String> updateUserLogtime(
            @PathVariable String name,
            @RequestBody Map<String, Integer> body
    ) {
        Integer monthlyLogtime = body.get("monthlyLogtime");

        if (monthlyLogtime == null || monthlyLogtime < 0) {
            return ResponseEntity.badRequest().body("유효하지 않은 시간 값입니다.");
        }

        adminService.updateUserLogtime(name, monthlyLogtime);
        return ResponseEntity.ok("로그타임 수정 완료");
    }

    @PatchMapping("/cabinets/{visibleNum}")
    public ResponseEntity<String> updateCabinet(
            @PathVariable Integer visibleNum,
            @RequestBody CabinetStatusRequest request
    ) {
        adminService.updateCabinetStatus(visibleNum, request);
        return ResponseEntity.ok("사물함 상태 변경 완료");
    }

    @PostMapping("/cabinets/{visibleNum}/force-return")
    public ResponseEntity<String> forceReturn(@PathVariable Integer visibleNum) {
        adminService.forceReturn(visibleNum);
        return ResponseEntity.ok("강제 반납 완료 (상태: 사용불가)");
    }
}
