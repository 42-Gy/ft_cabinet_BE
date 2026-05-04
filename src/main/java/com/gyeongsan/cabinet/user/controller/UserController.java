package com.gyeongsan.cabinet.user.controller;

import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import com.gyeongsan.cabinet.common.ApiResponse;
import com.gyeongsan.cabinet.domain.user.port.in.UserUseCase;
import com.gyeongsan.cabinet.user.dto.MyProfileResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v4/users")
@RequiredArgsConstructor
public class UserController {

    private final UserUseCase userUseCase;

    @GetMapping("/me")
    public ApiResponse<MyProfileResponseDto> getMyProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        MyProfileResponseDto myProfile = userUseCase.getMyProfile(userPrincipal.getUserId());
        return ApiResponse.success(myProfile);
    }

    @PostMapping("/attendance")
    public ApiResponse<String> doAttendance(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        userUseCase.doAttendance(userPrincipal.getUserId());
        return ApiResponse.success("출석체크 완료! 100 씨앗이 지급되었습니다. 🌱");
    }

    @GetMapping("/attendance")
    public ApiResponse<List<LocalDate>> getAttendanceCalendar(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<LocalDate> dates = userUseCase.getMyAttendanceDates(userPrincipal.getUserId());
        return ApiResponse.success(dates);
    }
}
