package com.gyeongsan.cabinet.domain.item.port.in;

import com.gyeongsan.cabinet.item.dto.ItemResponseDto;

import java.util.List;

public interface StoreUseCase {

    List<ItemResponseDto> getItems();

    void buyItem(Long userId, Long itemId);
}
