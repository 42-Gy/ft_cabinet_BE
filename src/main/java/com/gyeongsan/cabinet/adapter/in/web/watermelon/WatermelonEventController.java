package com.gyeongsan.cabinet.adapter.in.web.watermelon;

import com.gyeongsan.cabinet.adapter.in.web.watermelon.dto.*;
import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import com.gyeongsan.cabinet.common.ApiResponse;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.domain.watermelon.domain.Watermelon;
import com.gyeongsan.cabinet.domain.watermelon.domain.WatermelonConfig;
import com.gyeongsan.cabinet.domain.watermelon.domain.WatermelonEnhanceResult;
import com.gyeongsan.cabinet.domain.watermelon.domain.WatermelonItem;
import com.gyeongsan.cabinet.domain.watermelon.domain.WatermelonEventLog;
import com.gyeongsan.cabinet.domain.watermelon.port.in.BuyWatermelonItemUseCase;
import com.gyeongsan.cabinet.domain.watermelon.port.in.EnhanceWatermelonUseCase;
import com.gyeongsan.cabinet.domain.watermelon.port.in.GetWatermelonLeaderboardUseCase;
import com.gyeongsan.cabinet.domain.watermelon.port.in.GetWatermelonStatusUseCase;
import com.gyeongsan.cabinet.domain.watermelon.port.out.WatermelonEventLogRepositoryPort;
import com.gyeongsan.cabinet.domain.watermelon.port.out.WatermelonRepositoryPort;
import com.gyeongsan.cabinet.user.domain.User;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v4/watermelon-event")
@RateLimiter(name = "userApi")
public class WatermelonEventController {

    private final GetWatermelonStatusUseCase getStatusUseCase;
    private final EnhanceWatermelonUseCase enhanceUseCase;
    private final GetWatermelonLeaderboardUseCase getLeaderboardUseCase;
    private final BuyWatermelonItemUseCase buyItemUseCase;
    private final UserRepositoryPort userRepository;
    private final WatermelonRepositoryPort watermelonRepository;
    private final WatermelonEventLogRepositoryPort logRepository;

    @GetMapping("/me")
    public ApiResponse<WatermelonStatusResponse> getMyStatus(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        Watermelon wm = getStatusUseCase.getStatus(userId);
        long rank = watermelonRepository.findRankByUserId(userId);

        WatermelonStatusResponse response = WatermelonStatusResponse.builder()
                .userId(userId)
                .username(user.getName())
                .currentLevel(wm.getCurrentLevel())
                .highestLevel(wm.getHighestLevel())
                .highestLevelAchievedAt(wm.getHighestLevelAchievedAt())
                .seedBalance(user.getCoin())
                .rank(rank)
                .totalAttempts(wm.getTotalAttempts())
                .totalSuccesses(wm.getTotalSuccesses())
                .totalMaintains(wm.getTotalMaintains())
                .totalDrops(wm.getTotalDrops())
                .totalDestroys(wm.getTotalDestroys())
                .dropProtectionCount(wm.getDropProtectionCount())
                .destroyProtectionCount(wm.getDestroyProtectionCount())
                .premiumFertilizerCount(wm.getPremiumFertilizerCount())
                .dangerousFertilizerCount(wm.getDangerousFertilizerCount())
                .build();

        return ApiResponse.success(response);
    }

    @PostMapping("/enhance")
    public ApiResponse<EnhanceResponse> enhance(
            @RequestBody EnhanceRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getUserId();

        WatermelonEnhanceResult result = enhanceUseCase.enhance(
                userId,
                request.isUsePremium(),
                request.isUseDangerous(),
                request.isUseDropProj(),
                request.isUseDestroyProj()
        );

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        EnhanceResponse response = EnhanceResponse.builder()
                .beforeLevel(result.getBeforeLevel())
                .afterLevel(result.getAfterLevel())
                .rawOutcome(result.getRawOutcome())
                .finalOutcome(result.getFinalOutcome())
                .seedBalance(user.getCoin())
                .dropProtectionCount(result.getWatermelon().getDropProtectionCount())
                .destroyProtectionCount(result.getWatermelon().getDestroyProtectionCount())
                .premiumFertilizerCount(result.getWatermelon().getPremiumFertilizerCount())
                .dangerousFertilizerCount(result.getWatermelon().getDangerousFertilizerCount())
                .build();

        return ApiResponse.success(response);
    }

    @PostMapping("/shop/buy")
    public ApiResponse<BuyItemResponse> buyItem(
            @RequestBody BuyItemRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getUserId();

        Watermelon wm = buyItemUseCase.buyItem(userId, request.getItem(), request.getQuantity());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        BuyItemResponse response = BuyItemResponse.builder()
                .seedBalance(user.getCoin())
                .dropProtectionCount(wm.getDropProtectionCount())
                .destroyProtectionCount(wm.getDestroyProtectionCount())
                .premiumFertilizerCount(wm.getPremiumFertilizerCount())
                .dangerousFertilizerCount(wm.getDangerousFertilizerCount())
                .build();

        return ApiResponse.success(response);
    }

    @GetMapping("/rankings")
    public ApiResponse<Page<LeaderboardResponse>> getRankings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Watermelon> leaderboard = getLeaderboardUseCase.getLeaderboard(pageable);

        Page<LeaderboardResponse> response = leaderboard.map(wm -> {
            String name = userRepository.findById(wm.getUserId())
                    .map(User::getName)
                    .orElse("Unknown");
            return LeaderboardResponse.builder()
                    .userId(wm.getUserId())
                    .username(name)
                    .highestLevel(wm.getHighestLevel())
                    .highestLevelAchievedAt(wm.getHighestLevelAchievedAt())
                    .totalAttempts(wm.getTotalAttempts())
                    .build();
        });

        return ApiResponse.success(response);
    }

    @GetMapping("/logs")
    public ApiResponse<List<EnhancementLogResponse>> getLogs() {
        List<WatermelonEventLog> logs = logRepository.findAllRecentLogs();

        List<User> allUsers = userRepository.findAll();
        Map<Long, String> userIdToNameMap = allUsers.stream()
                .collect(Collectors.toMap(User::getId, User::getName, (existing, replacement) -> existing));

        List<EnhancementLogResponse> response = logs.stream()
                .map(log -> EnhancementLogResponse.builder()
                        .id(log.getId())
                        .userId(log.getUserId())
                        .userName(userIdToNameMap.getOrDefault(log.getUserId(), "Unknown"))
                        .beforeLevel(log.getBeforeLevel())
                        .afterLevel(log.getAfterLevel())
                        .usedPremiumFertilizer(log.isUsedPremiumFertilizer())
                        .usedDangerousFertilizer(log.isUsedDangerousFertilizer())
                        .usedDropProtection(log.isUsedDropProtection())
                        .usedDestroyProtection(log.isUsedDestroyProtection())
                        .rawOutcome(log.getRawOutcome())
                        .finalOutcome(log.getFinalOutcome())
                        .costSeeds(log.getCostSeeds())
                        .createdAt(log.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.success(response);
    }

    @GetMapping("/config")
    public ApiResponse<WatermelonConfigResponse> getConfig() {
        List<Integer> costs = new ArrayList<>();
        List<Double> success = new ArrayList<>();
        List<Double> maintain = new ArrayList<>();
        List<Double> drop = new ArrayList<>();
        List<Double> destroy = new ArrayList<>();

        for (int i = 0; i < WatermelonConfig.MAX_LEVEL; i++) {
            costs.add(WatermelonConfig.getCost(i));
            success.add(WatermelonConfig.getSuccessRate(i));
            maintain.add(WatermelonConfig.getMaintainRate(i));
            drop.add(WatermelonConfig.getDropRate(i));
            destroy.add(WatermelonConfig.getDestroyRate(i));
        }

        Map<String, Integer> itemPrices = new HashMap<>();
        for (WatermelonItem item : WatermelonItem.values()) {
            itemPrices.put(item.name(), item.getPrice());
        }

        WatermelonConfigResponse response = WatermelonConfigResponse.builder()
                .maxLevel(WatermelonConfig.MAX_LEVEL)
                .enhanceCosts(costs)
                .successRates(success)
                .maintainRates(maintain)
                .dropRates(drop)
                .destroyRates(destroy)
                .itemPrices(itemPrices)
                .build();

        return ApiResponse.success(response);
    }
}
