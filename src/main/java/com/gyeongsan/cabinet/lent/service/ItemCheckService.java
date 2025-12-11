package com.gyeongsan.cabinet.lent.service;

import com.gyeongsan.cabinet.item.domain.ItemHistory;
import com.gyeongsan.cabinet.item.domain.ItemType; // ItemType import 필요
import com.gyeongsan.cabinet.item.repository.ItemHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemCheckService {

    private final ItemHistoryRepository itemHistoryRepository;

    @Transactional(readOnly = true)
    // 👇 [복구] ItemType을 인자로 받도록 수정합니다.
    public List<ItemHistory> getUnusedLentTickets(Long userId, ItemType itemType) {
        return itemHistoryRepository.findUnusedItems(userId, itemType);
    }
}