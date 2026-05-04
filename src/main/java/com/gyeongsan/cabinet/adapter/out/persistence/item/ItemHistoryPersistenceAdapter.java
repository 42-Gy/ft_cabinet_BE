package com.gyeongsan.cabinet.adapter.out.persistence.item;

import com.gyeongsan.cabinet.domain.item.port.out.ItemHistoryRepositoryPort;
import com.gyeongsan.cabinet.item.domain.ItemHistory;
import com.gyeongsan.cabinet.item.domain.ItemType;
import com.gyeongsan.cabinet.item.repository.ItemHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ItemHistoryPersistenceAdapter implements ItemHistoryRepositoryPort {

    private final ItemHistoryRepository itemHistoryRepository;

    @Override
    public List<ItemHistory> findUnusedItems(Long userId, ItemType itemType) {
        return itemHistoryRepository.findUnusedItems(userId, itemType);
    }

    @Override
    public List<ItemHistory> findAllByUserIdAndUsedAtIsNull(Long userId) {
        return itemHistoryRepository.findAllByUserIdAndUsedAtIsNull(userId);
    }

    @Override
    public List<ItemHistory> findAllByUserIdOrderByPurchaseAtDesc(Long userId) {
        return itemHistoryRepository.findAllByUserIdOrderByPurchaseAtDesc(userId);
    }

    @Override
    public int countByUserIdAndItemTypeAndUsedAtIsNull(Long userId, ItemType itemType) {
        return itemHistoryRepository.countByUserIdAndItem_TypeAndUsedAtIsNull(userId, itemType);
    }

    @Override
    public int countByUserIdAndItemTypeAndPurchaseAtBetween(Long userId, ItemType itemType, LocalDateTime start, LocalDateTime end) {
        return itemHistoryRepository.countByUserIdAndItem_TypeAndPurchaseAtBetween(userId, itemType, start, end);
    }

    @Override
    public Long sumUsedItemPrice() {
        return itemHistoryRepository.sumUsedItemPrice();
    }

    @Override
    public List<Object[]> findItemSales() {
        return itemHistoryRepository.findItemSales();
    }

    @Override
    public List<Object[]> findItemUsageStats() {
        return itemHistoryRepository.findItemUsageStats();
    }

    @Override
    public ItemHistory save(ItemHistory itemHistory) {
        return itemHistoryRepository.save(itemHistory);
    }

    @Override
    public List<ItemHistory> saveAll(List<ItemHistory> itemHistories) {
        return itemHistoryRepository.saveAll(itemHistories);
    }

    @Override
    public void deleteAll(List<ItemHistory> itemHistories) {
        itemHistoryRepository.deleteAll(itemHistories);
    }
}
