package com.gyeongsan.cabinet.domain.watermelon.port.in;

import com.gyeongsan.cabinet.domain.watermelon.domain.Watermelon;
import com.gyeongsan.cabinet.domain.watermelon.domain.WatermelonItem;

public interface BuyWatermelonItemUseCase {
    Watermelon buyItem(Long userId, WatermelonItem item, int quantity);
}
