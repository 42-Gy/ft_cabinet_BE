package com.gyeongsan.cabinet.admin.controller;

import com.gyeongsan.cabinet.admin.dto.AdminUserDetailResponse;
import com.gyeongsan.cabinet.admin.service.AdminService;
import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;
import com.gyeongsan.cabinet.user.scheduler.LogtimeScheduler; // 👈 스케줄러 import
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin") // ADMIN 권한이 필요한 기본 경로
public class AdminController {

    private final AdminService adminService;
    private final LogtimeScheduler logtimeScheduler; // 👈 스케줄러 주입

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

    /**
     * 👇 [추가] 코인 지급 스케줄러 강제 실행 (테스트용)
     * POST /admin/test/coins
     */
    @PostMapping("/test/coins")
    public String forceGiveCoins() {
        logtimeScheduler.rewardCoins(); // 기존에 만들어둔 로직 실행
        return "✅ [성공] 로그타임 기반 코인 지급 스케줄러 실행 완료! (서버 로그를 확인하세요)";
    }
}