package com.gyeongsan.cabinet.admin.controller;

import com.gyeongsan.cabinet.admin.dto.*;
import com.gyeongsan.cabinet.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @DeleteMapping("/users/{userId}/ban")
    public ResponseEntity<String> unbanUser(@PathVariable Long userId) {
        adminService.unbanUser(userId);
        return ResponseEntity.ok("밴 해제 완료");
    }

    @PostMapping("/users/{userId}/coin")
    public ResponseEntity<String> provideCoin(
            @PathVariable Long userId,
            @RequestBody CoinProvideRequest request
    ) {
        adminService.provideCoin(userId, request);
        return ResponseEntity.ok("코인 지급 완료");
    }

    @PatchMapping("/cabinets/{cabinetId}")
    public ResponseEntity<String> updateCabinet(
            @PathVariable Long cabinetId,
            @RequestBody CabinetStatusRequest request
    ) {
        adminService.updateCabinetStatus(cabinetId, request);
        return ResponseEntity.ok("사물함 상태 변경 완료");
    }

    @PostMapping("/cabinets/{cabinetId}/force-return")
    public ResponseEntity<String> forceReturn(@PathVariable Long cabinetId) {
        adminService.forceReturn(cabinetId);
        return ResponseEntity.ok("강제 반납 완료");
    }
}
