package com.gyeongsan.cabinet.domain.watermelon.service;

import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.domain.watermelon.domain.*;
import com.gyeongsan.cabinet.domain.watermelon.port.in.BuyWatermelonItemUseCase;
import com.gyeongsan.cabinet.domain.watermelon.port.in.EnhanceWatermelonUseCase;
import com.gyeongsan.cabinet.domain.watermelon.port.in.GetWatermelonLeaderboardUseCase;
import com.gyeongsan.cabinet.domain.watermelon.port.in.GetWatermelonStatusUseCase;
import com.gyeongsan.cabinet.domain.watermelon.port.out.WatermelonEventLogRepositoryPort;
import com.gyeongsan.cabinet.domain.watermelon.port.out.WatermelonRepositoryPort;
import com.gyeongsan.cabinet.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WatermelonEventService implements
        GetWatermelonStatusUseCase,
        EnhanceWatermelonUseCase,
        GetWatermelonLeaderboardUseCase,
        BuyWatermelonItemUseCase {

    private final WatermelonRepositoryPort watermelonRepository;
    private final WatermelonEventLogRepositoryPort logRepository;
    private final UserRepositoryPort userRepository;
    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    public Watermelon getStatus(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        return watermelonRepository.findByUserId(userId)
                .orElseGet(() -> watermelonRepository.save(Watermelon.createNew(userId)));
    }

    @Override
    @Transactional
    public Watermelon buyItem(Long userId, WatermelonItem item, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("구매 수량은 1개 이상이어야 합니다.");
        }

        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        Watermelon watermelon = watermelonRepository.findByUserId(userId)
                .orElseGet(() -> Watermelon.createNew(userId));

        long totalPrice = (long) item.getPrice() * quantity;
        user.useCoin(totalPrice);

        watermelon.buyItem(item, quantity);

        userRepository.save(user);
        return watermelonRepository.save(watermelon);
    }

    @Override
    @Transactional
    public WatermelonEnhanceResult enhance(Long userId, boolean usePremium, boolean useDangerous, boolean useDropProj, boolean useDestroyProj) {
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        Watermelon watermelon = watermelonRepository.findByUserId(userId)
                .orElseGet(() -> Watermelon.createNew(userId));

        int beforeLevel = watermelon.getCurrentLevel();
        if (beforeLevel >= WatermelonConfig.MAX_LEVEL) {
            throw new IllegalArgumentException("이미 최대 강화 단계에 도달했습니다.");
        }

        int cost = WatermelonConfig.getCost(beforeLevel);
        user.useCoin((long) cost);

        watermelon.useFertilizer(usePremium, useDangerous);

        EnhancementResult rawOutcome = rollEnhancement(beforeLevel, usePremium, useDangerous);

        EnhancementResult finalOutcome = watermelon.applyEnhancement(rawOutcome, useDropProj, useDestroyProj);

        userRepository.save(user);
        Watermelon savedWatermelon = watermelonRepository.save(watermelon);

        WatermelonEventLog eventLog = WatermelonEventLog.builder()
                .userId(userId)
                .beforeLevel(beforeLevel)
                .afterLevel(savedWatermelon.getCurrentLevel())
                .usedPremiumFertilizer(usePremium)
                .usedDangerousFertilizer(useDangerous)
                .usedDropProtection(useDropProj)
                .usedDestroyProtection(useDestroyProj)
                .rawOutcome(rawOutcome)
                .finalOutcome(finalOutcome)
                .costSeeds(cost)
                .createdAt(LocalDateTime.now())
                .build();
        logRepository.save(eventLog);

        return new WatermelonEnhanceResult(beforeLevel, savedWatermelon.getCurrentLevel(), rawOutcome, finalOutcome, savedWatermelon);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Watermelon> getLeaderboard(Pageable pageable) {
        return watermelonRepository.findAllOrderByRanking(pageable);
    }

    private EnhancementResult rollEnhancement(int level, boolean usePremium, boolean useDangerous) {
        double successRate = WatermelonConfig.getSuccessRate(level);
        double maintainRate = WatermelonConfig.getMaintainRate(level);
        double dropRate = WatermelonConfig.getDropRate(level);
        double destroyRate = WatermelonConfig.getDestroyRate(level);

        if (usePremium) {
            successRate += 0.05;
            double sum = maintainRate + dropRate + destroyRate;
            if (sum > 0) {
                double newFailureProb = 1.0 - successRate;
                maintainRate = (maintainRate * newFailureProb) / sum;
                dropRate = (dropRate * newFailureProb) / sum;
                destroyRate = (destroyRate * newFailureProb) / sum;
            }
        } else if (useDangerous) {
            successRate += 0.10;
            destroyRate += 0.10;
            double sum = maintainRate + dropRate;
            double newRemain = 1.0 - successRate - destroyRate;
            if (newRemain < 0) {
                successRate = 1.0 - destroyRate;
                maintainRate = 0.0;
                dropRate = 0.0;
            } else if (sum > 0) {
                maintainRate = (maintainRate * newRemain) / sum;
                dropRate = (dropRate * newRemain) / sum;
            }
        }

        double r = random.nextDouble();
        if (r < successRate) {
            return EnhancementResult.SUCCESS;
        } else if (r < successRate + maintainRate) {
            return EnhancementResult.MAINTAIN;
        } else if (r < successRate + maintainRate + dropRate) {
            return EnhancementResult.DROP;
        } else {
            return EnhancementResult.DESTROY;
        }
    }
}
