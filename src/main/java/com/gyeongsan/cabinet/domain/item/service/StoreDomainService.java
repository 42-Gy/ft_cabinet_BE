package com.gyeongsan.cabinet.domain.item.service;

import com.gyeongsan.cabinet.coin.domain.CoinHistory;
import com.gyeongsan.cabinet.coin.domain.CoinLogType;
import com.gyeongsan.cabinet.domain.coin.port.out.CoinHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.item.port.in.StoreUseCase;
import com.gyeongsan.cabinet.domain.item.port.out.ItemHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.item.port.out.ItemRepositoryPort;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import com.gyeongsan.cabinet.item.domain.Item;
import com.gyeongsan.cabinet.item.domain.ItemHistory;
import com.gyeongsan.cabinet.item.domain.ItemType;
import com.gyeongsan.cabinet.item.dto.ItemResponseDto;
import com.gyeongsan.cabinet.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class StoreDomainService implements StoreUseCase {

    private static final int MAX_EXTENSION_COUNT = 5;

    private final UserRepositoryPort userRepository;
    private final ItemRepositoryPort itemRepository;
    private final ItemHistoryRepositoryPort itemHistoryRepository;
    private final CoinHistoryRepositoryPort coinHistoryRepository;

    @Override
    public List<ItemResponseDto> getItems() {
        return itemRepository.findAll().stream()
                .map(ItemResponseDto::new)
                .toList();
    }

    @Override
    @Transactional
    public void buyItem(Long userId, Long itemId) {
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ServiceException(ErrorCode.ITEM_NOT_FOUND));

        if (item.getType() == ItemType.LENT) {
            throw new IllegalArgumentException("대여권은 상점에서 구매할 수 없습니다. (월 50시간 학습 보상으로만 획득 가능)");
        }

        if (item.getType() == ItemType.EXTENSION) {
            validateExtensionPurchase(userId);
        }

        log.info("구매 요청 - 유저: {}, 아이템: {}, 가격: {}", user.getName(), item.getName(), item.getPrice());

        if (user.getCoin() < item.getPrice()) {
            throw new ServiceException(ErrorCode.NOT_ENOUGH_COIN);
        }

        user.useCoin(item.getPrice());

        CoinHistory coinUsage = CoinHistory.of(user, -item.getPrice(), CoinLogType.ITEM_PURCHASE, item.getName());
        coinHistoryRepository.save(coinUsage);

        ItemHistory history = new ItemHistory(LocalDateTime.now(), null, user, item);
        itemHistoryRepository.save(history);

        log.info("구매 성공! 남은 씨앗: {}", user.getCoin());
    }

    private void validateExtensionPurchase(Long userId) {
        int currentCount = itemHistoryRepository.countByUserIdAndItemTypeAndUsedAtIsNull(userId, ItemType.EXTENSION);
        if (currentCount >= MAX_EXTENSION_COUNT) {
            throw new ServiceException(ErrorCode.EXTENSION_ITEM_LIMIT_EXCEEDED);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime firstDayOfMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        int monthlyPurchaseCount = itemHistoryRepository.countByUserIdAndItemTypeAndPurchaseAtBetween(
                userId, ItemType.EXTENSION, firstDayOfMonth, now);

        if (monthlyPurchaseCount >= MAX_EXTENSION_COUNT) {
            throw new ServiceException(ErrorCode.EXTENSION_ITEM_PURCHASE_LIMIT_EXCEEDED);
        }
    }
}
