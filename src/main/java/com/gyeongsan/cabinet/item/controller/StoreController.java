package com.gyeongsan.cabinet.item.controller;

import com.gyeongsan.cabinet.item.service.StoreService;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v4/store")
public class StoreController {

    private final StoreService storeService;
    private final UserRepository userRepository;

    @PostMapping("/buy/{itemId}")
    public String buyItem(@PathVariable Long itemId, @AuthenticationPrincipal OAuth2User principal) {
        String intraName = principal.getAttribute("login");
        User user = userRepository.findByName(intraName)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 유저입니다."));

        storeService.buyItem(user.getId(), itemId);

        return "✅ " + intraName + "님, 아이템 구매 성공!";
    }
}