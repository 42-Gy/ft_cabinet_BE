package com.gyeongsan.cabinet.item.controller;

import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import com.gyeongsan.cabinet.common.ApiResponse;
import com.gyeongsan.cabinet.item.dto.ItemResponseDto;
import com.gyeongsan.cabinet.item.service.StoreService;
import com.gyeongsan.cabinet.user.domain.User;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v4/store")
@RateLimiter(name = "userApi")
public class StoreController {

    private final StoreService storeService;
    private final UserRepository userRepository;

    @GetMapping("/items")
    public ApiResponse<List<ItemResponseDto>> getItems() {
        return ApiResponse.success(storeService.getItems());
    }

    @PostMapping("/buy/{itemId}")
    public ApiResponse<String> buyItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 유저입니다."));

        storeService.buyItem(userId, itemId);

        return ApiResponse.success("✅ " + user.getName() + "님, 아이템 구매 성공!");
    }
}
