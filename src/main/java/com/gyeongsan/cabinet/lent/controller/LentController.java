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

import java.security.Principal; // 👈 중요: OAuth2User 대신 Principal 사용

@RestController
@RequiredArgsConstructor
@RequestMapping("/v4/lent")
@Log4j2
public class LentController {

    private final LentFacadeService lentFacadeService;
    private final UserRepository userRepository;

    @PostMapping("/cabinets/{cabinetId}")
    // 👇 [수정] OAuth2User -> Principal (토큰에서 유저 ID 추출)
    public MessageResponse startLentCabinet(@PathVariable Long cabinetId, Principal principal) {
        // 1. 토큰의 Subject(유저 ID)를 파싱
        Long userId = Long.valueOf(principal.getName());

        // 2. 유저 조회 (이름을 응답 메시지에 쓰기 위해)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 유저입니다."));

        // 3. 대여 서비스 호출
        lentFacadeService.startLentCabinet(userId, cabinetId);

        return new MessageResponse("✅ " + user.getName() + "님, " + cabinetId + "번 사물함 대여 성공!");
    }

    @PostMapping("/return")
    // 👇 [수정] OAuth2User -> Principal
    public MessageResponse endLentCabinet(Principal principal) {
        Long userId = Long.valueOf(principal.getName());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 유저입니다."));

        // 4. 반납 서비스 호출
        lentFacadeService.endLentCabinet(userId);

        return new MessageResponse("✅ " + user.getName() + "님, 반납 성공!");
    }
}