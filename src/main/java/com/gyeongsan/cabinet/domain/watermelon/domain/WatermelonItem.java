package com.gyeongsan.cabinet.domain.watermelon.domain;

import lombok.Getter;

@Getter
public enum WatermelonItem {
    DROP_PROTECTION("하락 방지권", 400),
    DESTROY_PROTECTION("파괴 방지권", 500),
    PREMIUM_FERTILIZER("프리미엄 비료", 1000),
    DANGEROUS_FERTILIZER("위험한 비료", 700);

    private final String name;
    private final int price;

    WatermelonItem(String name, int price) {
        this.name = name;
        this.price = price;
    }
}
