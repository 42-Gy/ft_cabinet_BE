package com.gyeongsan.cabinet.domain.watermelon.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class Watermelon {

    private final Long userId;
    private int currentLevel;
    private int highestLevel;
    private LocalDateTime highestLevelAchievedAt;

    private int totalAttempts;
    private int totalSuccesses;
    private int totalMaintains;
    private int totalDrops;
    private int totalDestroys;

    private int dropProtectionCount;
    private int destroyProtectionCount;
    private int premiumFertilizerCount;
    private int dangerousFertilizerCount;

    public static Watermelon createNew(Long userId) {
        return Watermelon.builder()
                .userId(userId)
                .currentLevel(0)
                .highestLevel(0)
                .highestLevelAchievedAt(LocalDateTime.now())
                .totalAttempts(0)
                .totalSuccesses(0)
                .totalMaintains(0)
                .totalDrops(0)
                .totalDestroys(0)
                .dropProtectionCount(0)
                .destroyProtectionCount(0)
                .premiumFertilizerCount(0)
                .dangerousFertilizerCount(0)
                .build();
    }

    public void buyItem(WatermelonItem item, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("구매 수량은 1개 이상이어야 합니다.");
        }
        switch (item) {
            case DROP_PROTECTION -> this.dropProtectionCount += quantity;
            case DESTROY_PROTECTION -> this.destroyProtectionCount += quantity;
            case PREMIUM_FERTILIZER -> this.premiumFertilizerCount += quantity;
            case DANGEROUS_FERTILIZER -> this.dangerousFertilizerCount += quantity;
        }
    }

    public void useFertilizer(boolean usePremium, boolean useDangerous) {
        if (usePremium && useDangerous) {
            throw new IllegalArgumentException("비료는 동시에 1개만 사용할 수 있습니다.");
        }
        if (usePremium) {
            if (this.premiumFertilizerCount < 1) {
                throw new IllegalArgumentException("프리미엄 비료가 부족합니다.");
            }
            this.premiumFertilizerCount--;
        }
        if (useDangerous) {
            if (this.currentLevel >= 7) {
                throw new IllegalArgumentException("위험한 비료는 7강 이상에서는 사용할 수 없습니다.");
            }
            if (this.dangerousFertilizerCount < 1) {
                throw new IllegalArgumentException("위험한 비료가 부족합니다.");
            }
            this.dangerousFertilizerCount--;
        }
    }

    public void applyEnhancement(EnhancementResult rawResult, boolean useDropProj, boolean useDestroyProj) {
        if (this.currentLevel >= WatermelonConfig.MAX_LEVEL) {
            throw new IllegalArgumentException("이미 최대 강화 단계에 도달했습니다.");
        }

        EnhancementResult finalResult = rawResult;
        boolean consumedDropTicket = false;
        boolean consumedDestroyTicket = false;

        if (useDropProj) {
            if (this.dropProtectionCount < 1) {
                throw new IllegalArgumentException("하락 방지권이 부족합니다.");
            }
            this.dropProtectionCount--;
            consumedDropTicket = true;
        }

        if (useDestroyProj) {
            if (this.destroyProtectionCount < 1) {
                throw new IllegalArgumentException("파괴 방지권이 부족합니다.");
            }
            this.destroyProtectionCount--;
            consumedDestroyTicket = true;
        }

        if (rawResult == EnhancementResult.DROP && consumedDropTicket) {
            finalResult = EnhancementResult.MAINTAIN;
        } else if (rawResult == EnhancementResult.DESTROY && consumedDestroyTicket) {
            finalResult = EnhancementResult.MAINTAIN;
        }

        this.totalAttempts++;

        switch (finalResult) {
            case SUCCESS -> {
                this.currentLevel++;
                this.totalSuccesses++;
                if (this.currentLevel > this.highestLevel) {
                    this.highestLevel = this.currentLevel;
                    this.highestLevelAchievedAt = LocalDateTime.now();
                }
            }
            case MAINTAIN -> {
                if (rawResult == EnhancementResult.DESTROY && consumedDestroyTicket) {
                    this.currentLevel = Math.max(0, this.currentLevel - 2);
                }
                this.totalMaintains++;
            }
            case DROP -> {
                this.currentLevel = Math.max(0, this.currentLevel - 1);
                this.totalDrops++;
            }
            case DESTROY -> {
                this.currentLevel = 0;
                this.totalDestroys++;
            }
        }
    }
}
