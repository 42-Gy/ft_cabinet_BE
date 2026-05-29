package com.gyeongsan.cabinet.domain.watermelon.domain;

import lombok.Getter;

@Getter
public class WatermelonConfig {

    public static final int MAX_LEVEL = 10;

    private static final int[] ENHANCE_COSTS = {
        100, 150, 200, 300, 400, 500, 600, 800, 1000, 1500
    };

    private static final double[] SUCCESS_RATES = {
        0.90, 0.80, 0.70, 0.60, 0.50, 0.45, 0.40, 0.30, 0.20, 0.10
    };

    private static final double[] MAINTAIN_RATES = {
        0.10, 0.20, 0.15, 0.20, 0.25, 0.25, 0.25, 0.25, 0.30, 0.40
    };

    private static final double[] DROP_RATES = {
        0.00, 0.00, 0.15, 0.20, 0.20, 0.25, 0.25, 0.30, 0.35, 0.35
    };

    private static final double[] DESTROY_RATES = {
        0.00, 0.00, 0.00, 0.00, 0.05, 0.05, 0.10, 0.15, 0.15, 0.15
    };

    public static int getCost(int level) {
        if (level < 0 || level >= MAX_LEVEL) {
            throw new IllegalArgumentException("올바르지 않은 강화 레벨입니다.");
        }
        return ENHANCE_COSTS[level];
    }

    public static double getSuccessRate(int level) {
        if (level < 0 || level >= MAX_LEVEL) {
            throw new IllegalArgumentException("올바르지 않은 강화 레벨입니다.");
        }
        return SUCCESS_RATES[level];
    }

    public static double getMaintainRate(int level) {
        if (level < 0 || level >= MAX_LEVEL) {
            throw new IllegalArgumentException("올바르지 않은 강화 레벨입니다.");
        }
        return MAINTAIN_RATES[level];
    }

    public static double getDropRate(int level) {
        if (level < 0 || level >= MAX_LEVEL) {
            throw new IllegalArgumentException("올바르지 않은 강화 레벨입니다.");
        }
        return DROP_RATES[level];
    }

    public static double getDestroyRate(int level) {
        if (level < 0 || level >= MAX_LEVEL) {
            throw new IllegalArgumentException("올바르지 않은 강화 레벨입니다.");
        }
        return DESTROY_RATES[level];
    }
}
