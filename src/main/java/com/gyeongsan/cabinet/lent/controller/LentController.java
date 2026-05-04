package com.gyeongsan.cabinet.lent.controller;

import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import com.gyeongsan.cabinet.common.ApiResponse;
import com.gyeongsan.cabinet.common.dto.MessageResponse;
import com.gyeongsan.cabinet.domain.lent.port.in.LentUseCase;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.lent.dto.LentExtensionRequest;
import com.gyeongsan.cabinet.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v4/lent")
@Log4j2
public class LentController {

    private final LentUseCase lentUseCase;
    private final UserRepositoryPort userRepository;

    @PostMapping("/reservation/{visibleNum}")
    public ApiResponse<MessageResponse> makeReservation(
            @PathVariable Integer visibleNum,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getUserId();
        lentUseCase.makeReservation(userId, visibleNum);
        return ApiResponse.success(new MessageResponse("✅ " + visibleNum + "번 사물함 예약 완료! (15분간 선점)"));
    }

    @PostMapping("/cabinets/{visibleNum}")
    public ApiResponse<MessageResponse> startLentCabinet(
            @PathVariable Integer visibleNum,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 유저입니다."));

        lentUseCase.startLent(userId, visibleNum);

        return ApiResponse.success(new MessageResponse(
                "✅ " + user.getName() + "님, " + visibleNum + "번 사물함 대여 성공!"));
    }

    @PostMapping(value = "/check-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MessageResponse> checkLentCabinetImage(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        lentUseCase.checkLentCabinetImage(userPrincipal.getUserId(), file);
        return ApiResponse.success(new MessageResponse("✅ AI 검증 통과! 반납을 계속 진행해주세요."));
    }

    @PostMapping(value = "/return", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MessageResponse> endLentCabinet(
            @RequestPart("file") MultipartFile file,
            @RequestParam("previousPassword") String previousPassword,
            @RequestParam("forceReturn") Boolean forceReturn,
            @RequestParam(value = "reason", required = false) String reason,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 유저입니다."));

        if (previousPassword == null || !previousPassword.matches("\\d{4}")) {
            throw new IllegalArgumentException("비밀번호는 4자리 숫자여야 합니다.");
        }

        lentUseCase.endLent(userId, previousPassword, file, forceReturn, reason);

        if (forceReturn) {
            return ApiResponse.success(new MessageResponse(
                    "✅ " + user.getName() + "님, 수동 반납 접수 완료. (AI 검사 실패로 승인 요청)"));
        }
        return ApiResponse.success(new MessageResponse(
                "✅ " + user.getName() + "님, 반납 성공! (AI 청결도 검사 통과 🧹)"));
    }

    @PostMapping("/extension")
    public ApiResponse<MessageResponse> useExtension(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getUserId();
        lentUseCase.useExtension(userId);
        return ApiResponse.success(new MessageResponse("✅ 대여 기간이 3일 연장되었습니다! 🎉"));
    }

    @PatchMapping("/extension/auto")
    public ApiResponse<MessageResponse> updateAutoExtension(
            @RequestBody LentExtensionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        lentUseCase.updateAutoExtensionStatus(userPrincipal.getUserId(), request.enabled());
        String status = request.enabled() ? "ON" : "OFF";
        return ApiResponse.success(new MessageResponse("✅ 자동 연장 설정이 " + status + "로 변경되었습니다."));
    }

    @PostMapping("/renew")
    public ApiResponse<MessageResponse> manualRenew(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getUserId();
        lentUseCase.manualRenew(userId);
        return ApiResponse.success(new MessageResponse("✅ 대여권을 사용하여 대여 기간이 31일 연장되었습니다! 🎉"));
    }

    @PostMapping(value = "/swap/{newVisibleNum}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MessageResponse> useSwap(
            @PathVariable Integer newVisibleNum,
            @RequestPart("file") MultipartFile file,
            @RequestParam("previousPassword") String previousPassword,
            @RequestParam("forceReturn") Boolean forceReturn,
            @RequestParam(value = "reason", required = false) String reason,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getUserId();

        if (previousPassword == null || !previousPassword.matches("\\d{4}")) {
            throw new IllegalArgumentException("비밀번호는 4자리 숫자여야 합니다.");
        }

        lentUseCase.useSwap(userId, newVisibleNum, previousPassword, file, forceReturn, reason);

        if (forceReturn) {
            return ApiResponse.success(new MessageResponse("✅ 수동 이사 접수 완료. (AI 검사 실패로 승인 요청) 🚚"));
        }
        return ApiResponse.success(new MessageResponse("✅ 사물함 이사 완료! (" + newVisibleNum + "번) 🚚"));
    }

    @PostMapping("/penalty-exemption")
    public ApiResponse<MessageResponse> usePenaltyExemption(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getUserId();
        lentUseCase.usePenaltyExemption(userId);
        return ApiResponse.success(new MessageResponse("✅ 패널티가 1일 감면되었습니다! (해방까지 파이팅 💪)"));
    }
}