package com.gyeongsan.cabinet.item.dto;

import com.gyeongsan.cabinet.item.domain.Item;
import com.gyeongsan.cabinet.item.domain.ItemType;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ItemResponseDto {

    private final Long itemId;
    private final String name;
    private final ItemType type;
    private final Long price;
    private final String description;

    public ItemResponseDto(Item item) {
        this.itemId = item.getId();
        this.name = item.getName();
        this.type = item.getType();
        this.price = item.getPrice();
        this.description = item.getDescription();
    }
}
