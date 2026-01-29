package com.gyeongsan.cabinet.user.controller;

import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import com.gyeongsan.cabinet.common.ApiResponse;
import com.gyeongsan.cabinet.user.dto.MyProfileResponseDto;
import com.gyeongsan.cabinet.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v4/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<MyProfileResponseDto> getMyProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        MyProfileResponseDto myProfile = userService.getMyProfile(userPrincipal.getUserId());
        return ApiResponse.success(myProfile);
    }

    @PostMapping("/attendance")
    public ApiResponse<String> doAttendance(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        userService.doAttendance(userPrincipal.getUserId());
        return ApiResponse.success("출석체크 완료! 100 씨앗이 지급되었습니다. 🌱");
    }

    @GetMapping("/attendance")
    public ApiResponse<List<LocalDate>> getAttendanceCalendar(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<LocalDate> dates = userService.getMyAttendanceDates(userPrincipal.getUserId());
        return ApiResponse.success(dates);
    }
}
