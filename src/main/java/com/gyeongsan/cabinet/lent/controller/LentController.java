package com.gyeongsan.cabinet.lent.controller;

import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import com.gyeongsan.cabinet.common.dto.MessageResponse;
import com.gyeongsan.cabinet.lent.service.LentFacadeService;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v4/lent")
@Log4j2
public class LentController {

    private final LentFacadeService lentFacadeService;
    private final UserRepository userRepository;

    @PostMapping("/cabinets/{cabinetId}")
    public MessageResponse startLentCabinet(
            @PathVariable Long cabinetId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 유저입니다."));

        lentFacadeService.startLentCabinet(userId, cabinetId);

        return new MessageResponse(
                "✅ " + user.getName() + "님, " + cabinetId + "번 사물함 대여 성공!"
        );
    }

    @PostMapping("/return")
    public MessageResponse endLentCabinet(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 유저입니다."));

        lentFacadeService.endLentCabinet(userId);

        return new MessageResponse("✅ " + user.getName() + "님, 반납 성공!");
    }

    @PostMapping("/extension")
    public MessageResponse useExtension(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();

        lentFacadeService.useExtension(userId);

        return new MessageResponse("✅ 대여 기간이 15일 연장되었습니다! 🎉");
    }

    @PostMapping("/swap/{newCabinetId}")
    public MessageResponse useSwap(
            @PathVariable Long newCabinetId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();

        lentFacadeService.useSwap(userId, newCabinetId);

        return new MessageResponse("✅ 사물함 이사 완료! (" + newCabinetId + "번)");
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
