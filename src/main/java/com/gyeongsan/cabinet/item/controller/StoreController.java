package com.gyeongsan.cabinet.item.controller;

import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import com.gyeongsan.cabinet.common.ApiResponse;
import com.gyeongsan.cabinet.domain.item.port.in.StoreUseCase;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.item.dto.ItemResponseDto;
import com.gyeongsan.cabinet.user.domain.User;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v4/store")
@RateLimiter(name = "userApi")
public class StoreController {

    private final StoreUseCase storeUseCase;
    private final UserRepositoryPort userRepository;

    @GetMapping("/items")
    public ApiResponse<List<ItemResponseDto>> getItems() {
        return ApiResponse.success(storeUseCase.getItems());
    }

    @PostMapping("/buy/{itemId}")
    public ApiResponse<String> buyItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 유저입니다."));

        storeUseCase.buyItem(userId, itemId);

        return ApiResponse.success("✅ " + user.getName() + "님, 아이템 구매 성공!");
    }
}
