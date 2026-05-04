package com.gyeongsan.cabinet.domain.item.port.out;

import com.gyeongsan.cabinet.item.domain.ItemHistory;
import com.gyeongsan.cabinet.item.domain.ItemType;

import java.time.LocalDateTime;
import java.util.List;

public interface ItemHistoryRepositoryPort {

    List<ItemHistory> findUnusedItems(Long userId, ItemType itemType);

    List<ItemHistory> findAllByUserIdAndUsedAtIsNull(Long userId);

    List<ItemHistory> findAllByUserIdOrderByPurchaseAtDesc(Long userId);

    int countByUserIdAndItemTypeAndUsedAtIsNull(Long userId, ItemType itemType);

    int countByUserIdAndItemTypeAndPurchaseAtBetween(Long userId, ItemType itemType, LocalDateTime start, LocalDateTime end);

    Long sumUsedItemPrice();

    List<Object[]> findItemSales();

    List<Object[]> findItemUsageStats();

    ItemHistory save(ItemHistory itemHistory);

    List<ItemHistory> saveAll(List<ItemHistory> itemHistories);

    void deleteAll(List<ItemHistory> itemHistories);
}
