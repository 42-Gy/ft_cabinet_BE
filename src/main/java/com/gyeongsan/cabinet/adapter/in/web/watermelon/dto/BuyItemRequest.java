package com.gyeongsan.cabinet.adapter.in.web.watermelon.dto;

import com.gyeongsan.cabinet.domain.watermelon.domain.WatermelonItem;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BuyItemRequest {
    private WatermelonItem item;
    private int quantity;
}
