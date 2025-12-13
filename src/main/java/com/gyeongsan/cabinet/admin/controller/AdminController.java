package com.gyeongsan.cabinet.admin.controller;

import com.gyeongsan.cabinet.admin.dto.AdminUserDetailResponse;
import com.gyeongsan.cabinet.admin.service.AdminService;
import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;
import com.gyeongsan.cabinet.user.scheduler.LogtimeScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final LogtimeScheduler logtimeScheduler;

    @PutMapping("/cabinet/{cabinetId}/status")
    public String changeCabinetStatus(
            @PathVariable Long cabinetId,
            @RequestParam("status") CabinetStatus status,
            @RequestParam("note") String note) {

        adminService.updateCabinetStatus(cabinetId, status, note);
        return "✅ " + cabinetId + "번 사물함 상태가 [" + status + "]로 변경되었습니다. (사유: " + note + ")";
    }

    @GetMapping("/users")
    public List<AdminUserDetailResponse> getAllUsers() {
        return adminService.findAllUsers();
    }

}
