package com.gyeongsan.cabinet.lent.controller;

import com.gyeongsan.cabinet.common.dto.MessageResponse; // 👈 [추가] MessageResponse DTO import
import com.gyeongsan.cabinet.lent.service.LentFacadeService;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
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
    // 👇 [수정] 반환 타입을 String에서 MessageResponse로 변경합니다.
    public MessageResponse startLentCabinet(@PathVariable Long cabinetId, @AuthenticationPrincipal OAuth2User principal) {
        String intraName = principal.getName();

        User user = userRepository.findByName(intraName)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 유저입니다."));

        // 원래의 ID 기반 호출
        lentFacadeService.startLentCabinet(user.getId(), cabinetId);

        // 👇 [수정] JSON MessageResponse 객체를 반환합니다.
        return new MessageResponse("✅ " + intraName + "님, " + cabinetId + "번 사물함 대여 성공!");
    }

    @PostMapping("/return")
    // 👇 [수정] 반환 타입을 String에서 MessageResponse로 변경합니다.
    public MessageResponse endLentCabinet(@AuthenticationPrincipal OAuth2User principal) {
        String intraName = principal.getName();

        User user = userRepository.findByName(intraName)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 유저입니다."));

        // 원래의 ID 기반 호출
        lentFacadeService.endLentCabinet(user.getId());

        // 👇 [수정] JSON MessageResponse 객체를 반환합니다.
        return new MessageResponse("✅ " + intraName + "님, 반납 성공!");
    }
}