package com.gyeongsan.cabinet.domain.watermelon.domain;

import lombok.Getter;

@Getter
public class WatermelonConfig {

    public static final int MAX_LEVEL = 10;

    private static final int[] ENHANCE_COSTS = {
        10, 20, 30, 40, 50, 80, 100, 200, 300, 500
    };

    private static final double[] SUCCESS_RATES = {
        0.95, 0.88, 0.78, 0.65, 0.54, 0.44, 0.35, 0.27, 0.20, 0.15
    };

    private static final double[] MAINTAIN_RATES = {
        0.05, 0.10, 0.15, 0.20, 0.22, 0.23, 0.24, 0.25, 0.25, 0.25
    };

    private static final double[] DROP_RATES = {
        0.00, 0.02, 0.05, 0.10, 0.17, 0.24, 0.29, 0.32, 0.34, 0.38
    };

    private static final double[] DESTROY_RATES = {
        0.00, 0.00, 0.02, 0.05, 0.07, 0.09, 0.12, 0.16, 0.21, 0.22
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
