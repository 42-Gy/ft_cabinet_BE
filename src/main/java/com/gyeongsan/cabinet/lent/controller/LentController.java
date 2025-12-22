package com.gyeongsan.cabinet.lent.controller;

import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import com.gyeongsan.cabinet.common.dto.MessageResponse;
import com.gyeongsan.cabinet.lent.dto.LentReturnRequest;
import com.gyeongsan.cabinet.lent.service.LentFacadeService;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import jakarta.validation.Valid;
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

    private final LentFacadeService lentFacadeService;
    private final UserRepository userRepository;

    @PostMapping("/cabinets/{visibleNum}")
    public MessageResponse startLentCabinet(
            @PathVariable Integer visibleNum,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 유저입니다."));

        lentFacadeService.startLentCabinet(userId, visibleNum);

        return new MessageResponse(
                "✅ " + user.getName() + "님, " + visibleNum + "번 사물함 대여 성공!"
        );
    }

    @PostMapping(value = "/return", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MessageResponse endLentCabinet(
            @RequestPart("file") MultipartFile file,
            @RequestParam("shareCode") String shareCode,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 유저입니다."));

        if (shareCode == null || shareCode.isBlank()) {
            shareCode = "0000";
        }

        lentFacadeService.endLentCabinet(userId, shareCode, file);

        return new MessageResponse(
                "✅ " + user.getName() + "님, 반납 성공! (AI 청결도 검사 통과 🧹)"
        );
    }

    @PostMapping("/return/manual")
    public MessageResponse endLentCabinetManual(
            @Valid @RequestBody LentReturnRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();

        String reason = (request.reason() != null) ? request.reason() : "AI 인식 실패 및 수동 요청";

        lentFacadeService.endLentCabinetManual(userId, request.shareCode(), reason);

        return new MessageResponse("✅ 수동 반납 요청이 접수되었습니다. 관리자 확인 후 처리됩니다.");
    }

    @PostMapping("/extension")
    public MessageResponse useExtension(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();
        lentFacadeService.useExtension(userId);
        return new MessageResponse("✅ 대여 기간이 15일 연장되었습니다! 🎉");
    }

    @PostMapping(value = "/swap/{newVisibleNum}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MessageResponse useSwap(
            @PathVariable Integer newVisibleNum,
            @RequestPart("file") MultipartFile file,
            @RequestParam("shareCode") String shareCode,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();
        if (shareCode == null || shareCode.isBlank()) shareCode = "0000";

        lentFacadeService.useSwap(userId, newVisibleNum, shareCode, file);

        return new MessageResponse("✅ 사물함 이사 완료! (" + newVisibleNum + "번) 🚚");
    }

    @PostMapping("/penalty-exemption")
    public MessageResponse usePenaltyExemption(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();
        lentFacadeService.usePenaltyExemption(userId);
        return new MessageResponse("✅ 패널티가 1일 감면되었습니다! (해방까지 파이팅 💪)");
    }
}