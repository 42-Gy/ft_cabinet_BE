package com.gyeongsan.cabinet.lent.controller;

import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import com.gyeongsan.cabinet.common.dto.MessageResponse;
import com.gyeongsan.cabinet.lent.service.LentFacadeService;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v4/lent")
@Log4j2
@Validated
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
            @RequestPart(value = "shareCode")
            @Size(min = 4, max = 4, message = "공유 비밀번호는 4자리여야 합니다.")
            @Pattern(regexp = "\\d{4}", message = "공유 비밀번호는 숫자만 가능합니다.")
            String shareCode,

            @RequestPart(value = "file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 유저입니다."));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("사물함 내부 사진을 첨부해주세요!");
        }

        lentFacadeService.endLentCabinetWithAi(userId, shareCode, file);

        return new MessageResponse(
                "✅ " + user.getName() + "님, 반납 성공! (AI 청결도 검사 통과 🧹)"
        );
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

            @RequestPart(value = "shareCode")
            @Size(min = 4, max = 4, message = "공유 비밀번호는 4자리여야 합니다.")
            @Pattern(regexp = "\\d{4}", message = "공유 비밀번호는 숫자만 가능합니다.")
            String shareCode,

            @RequestPart(value = "file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("기존 사물함의 내부 사진을 첨부해주세요!");
        }

        lentFacadeService.useSwap(userId, newVisibleNum, shareCode, file);

        return new MessageResponse("✅ AI 검사 통과! 사물함 이사 완료! (" + newVisibleNum + "번) 🚚");
    }

    @PostMapping("/penalty-exemption")
    public MessageResponse usePenaltyExemption(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();

        lentFacadeService.usePenaltyExemption(userId);

        return new MessageResponse("✅ 패널티가 2일 감면되었습니다! (해방까지 파이팅 💪)");
    }
}
