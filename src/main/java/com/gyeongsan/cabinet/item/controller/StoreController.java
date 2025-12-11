package com.gyeongsan.cabinet.item.controller;

import com.gyeongsan.cabinet.item.service.StoreService;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal; // 👈 중요: OAuth2User 대신 Principal 사용

@RestController
@RequiredArgsConstructor
@RequestMapping("/v4/store")
public class StoreController {

    private final StoreService storeService;
    private final UserRepository userRepository;

    @PostMapping("/buy/{itemId}")
    // 👇 [수정] 세션 방식(OAuth2User) -> 토큰 방식(Principal)으로 변경
    public String buyItem(@PathVariable Long itemId, Principal principal) {
        // 1. 토큰에서 userId 추출 (JwtTokenProvider에서 Subject로 넣은 값)
        Long userId = Long.valueOf(principal.getName());

        // 2. 유저 정보 조회 (결과 메시지에 이름을 띄우기 위해)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 유저입니다."));

        // 3. 아이템 구매 로직 실행
        storeService.buyItem(userId, itemId);

        return "✅ " + user.getName() + "님, 아이템 구매 성공!";
    }
}