package com.gyeongsan.cabinet.domain.item.service;

import com.gyeongsan.cabinet.adapter.out.persistence.item.ItemRepository;
import com.gyeongsan.cabinet.domain.item.model.Item;
import com.gyeongsan.cabinet.domain.item.model.ItemType;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Log4j2
public class ItemPriceInitializer {

    private final ItemRepository itemRepository;

    @Value("${cabinet.items.extension-price}")
    private Long extensionPrice;

    @Value("${cabinet.items.swap-price}")
    private Long swapPrice;

    @Value("${cabinet.items.penalty-exemption-price}")
    private Long penaltyExemptionPrice;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initItemPrices() {
        log.info("🛒 아이템 가격 동기화 시작...");

        updateOrCreateItem(ItemType.EXTENSION, "연장권", extensionPrice, "대여 기간을 연장합니다.");
        updateOrCreateItem(ItemType.SWAP, "이사권", swapPrice, "다른 사물함으로 이동합니다.");
        updateOrCreateItem(
                ItemType.PENALTY_EXEMPTION, "패널티 감면권", penaltyExemptionPrice, "패널티를 감면합니다.");
        updateOrCreateItem(ItemType.LENT, "대여권", 0L, "30일간 사물함을 대여합니다.");

        log.info("✅ 아이템 가격 동기화 완료!");
    }

    private void updateOrCreateItem(ItemType type, String name, Long price, String description) {
        Optional<Item> itemOpt =
                itemRepository.findAll().stream().filter(i -> i.getType() == type).findFirst();

        if (itemOpt.isPresent()) {
            Item item = itemOpt.get();
            if (!item.getPrice().equals(price)) {
                item.updatePrice(price);
                log.info(" - {} 가격 변경: -> {}", name, price);
            }
        } else {
            Item newItem = new Item(name, type, price, description);
            itemRepository.save(newItem);
            log.info(" - {} 신규 생성 (가격: {})", name, price);
        }
    }
}
