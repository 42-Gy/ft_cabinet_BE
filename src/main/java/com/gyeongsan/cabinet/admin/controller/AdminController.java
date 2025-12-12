package com.gyeongsan.cabinet.admin.controller;

import com.gyeongsan.cabinet.admin.dto.AdminUserDetailResponse; // 사용자 목록 조회 DTO import
import com.gyeongsan.cabinet.admin.service.AdminService;
import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List; // List import

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin") // ADMIN 권한이 필요한 기본 경로
public class AdminController {

    private final AdminService adminService;

    /**
     * 사물함 상태 강제 변경 (예: 고장 처리, 수리 완료)
     * PUT /admin/cabinet/{cabinetId}/status?status=BROKEN&note=손잡이
     */
    @PutMapping("/cabinet/{cabinetId}/status")
    public String changeCabinetStatus(
            @PathVariable Long cabinetId,
            @RequestParam("status") CabinetStatus status,
            @RequestParam("note") String note) {

        adminService.updateCabinetStatus(cabinetId, status, note);
        return "✅ " + cabinetId + "번 사물함 상태가 [" + status + "]로 변경되었습니다. (사유: " + note + ")";
    }

    /**
     * 전체 사용자 목록 조회 엔드포인트
     * GET /admin/users
     */
    @GetMapping("/users")
    public List<AdminUserDetailResponse> getAllUsers() {
        return adminService.findAllUsers();
    }
}