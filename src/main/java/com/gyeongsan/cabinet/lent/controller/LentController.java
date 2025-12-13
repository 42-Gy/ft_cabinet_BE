package com.gyeongsan.cabinet.lent.controller;

import com.gyeongsan.cabinet.common.dto.MessageResponse;
import com.gyeongsan.cabinet.lent.service.LentFacadeService;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v4/lent")
@Log4j2
public class LentController {

    private final LentFacadeService lentFacadeService;
    private final UserRepository userRepository;

    @PostMapping("/cabinets/{cabinetId}")
    public MessageResponse startLentCabinet(@PathVariable Long cabinetId, Principal principal) {
        Long userId = Long.valueOf(principal.getName());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 유저입니다."));

        lentFacadeService.startLentCabinet(userId, cabinetId);

        return new MessageResponse(
                "✅ " + user.getName() + "님, " + cabinetId + "번 사물함 대여 성공!"
        );
    }

    @PostMapping("/return")
    public MessageResponse endLentCabinet(Principal principal) {
        Long userId = Long.valueOf(principal.getName());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 유저입니다."));

        lentFacadeService.endLentCabinet(userId);

        return new MessageResponse("✅ " + user.getName() + "님, 반납 성공!");
    }

    @PostMapping("/extension")
    public MessageResponse useExtension(Principal principal) {
        Long userId = Long.valueOf(principal.getName());

        lentFacadeService.useExtension(userId);

        return new MessageResponse("✅ 대여 기간이 15일 연장되었습니다! 🎉");
    }

    @PostMapping("/swap/{newCabinetId}")
    public MessageResponse useSwap(@PathVariable Long newCabinetId, Principal principal) {
        Long userId = Long.valueOf(principal.getName());

        lentFacadeService.useSwap(userId, newCabinetId);

        return new MessageResponse("✅ 사물함 이사 완료! (" + newCabinetId + "번)");
    }

    @PostMapping("/penalty-exemption")
    public MessageResponse usePenaltyExemption(Principal principal) {
        Long userId = Long.valueOf(principal.getName());

        lentFacadeService.usePenaltyExemption(userId);

        return new MessageResponse("✅ 패널티가 2일 감면되었습니다! (해방까지 파이팅 💪)");
    }
}