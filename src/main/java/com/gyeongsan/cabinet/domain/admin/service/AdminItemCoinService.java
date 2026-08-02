package com.gyeongsan.cabinet.domain.admin.service;

import com.gyeongsan.cabinet.adapter.in.web.admin.dto.CoinProvideRequest;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.CoinRevokeRequest;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.ItemGrantRequest;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.ItemRevokeRequest;
import com.gyeongsan.cabinet.domain.admin.port.in.AdminItemCoinUseCase;
import com.gyeongsan.cabinet.domain.coin.model.CoinHistory;
import com.gyeongsan.cabinet.domain.coin.model.CoinLogType;
import com.gyeongsan.cabinet.domain.coin.port.out.CoinHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.item.model.Item;
import com.gyeongsan.cabinet.domain.item.model.ItemHistory;
import com.gyeongsan.cabinet.domain.item.port.out.ItemHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.item.port.out.ItemRepositoryPort;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminItemCoinService implements AdminItemCoinUseCase {

    private final UserRepositoryPort userRepository;
    private final ItemRepositoryPort itemRepository;
    private final ItemHistoryRepositoryPort itemHistoryRepository;
    private final CoinHistoryRepositoryPort coinHistoryRepository;

    @Override
    public void provideCoin(String username, CoinProvideRequest request) {
        User user =
                userRepository
                        .findByName(username)
                        .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        user.addCoin(request.amount());

        CoinHistory adminGrant =
                CoinHistory.of(user, request.amount(), CoinLogType.ADMIN_GRANT, "관리자 지급");
        coinHistoryRepository.save(adminGrant);
    }

    @Override
    public void updateItemPrice(String itemName, Long newPrice) {
        Item item =
                itemRepository
                        .findByName(itemName)
                        .orElseThrow(() -> new ServiceException(ErrorCode.ITEM_NOT_FOUND));

        if (newPrice < 0) {
            throw new IllegalArgumentException("가격은 0원 이상이어야 합니다.");
        }

        item.updatePrice(newPrice);
    }

    @Override
    public void grantItem(String username, ItemGrantRequest request) {
        User user =
                userRepository
                        .findByName(username)
                        .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        Item item =
                itemRepository
                        .findByName(request.itemName())
                        .orElseThrow(() -> new ServiceException(ErrorCode.ITEM_NOT_FOUND));

        int quantity =
                (request.quantity() == null || request.quantity() < 1) ? 1 : request.quantity();

        for (int i = 0; i < quantity; i++) {
            ItemHistory itemHistory = new ItemHistory(LocalDateTime.now(), null, user, item);
            itemHistoryRepository.save(itemHistory);
        }
    }

    @Override
    public void revokeUserItem(String username, ItemRevokeRequest request) {
        User user =
                userRepository
                        .findByName(username)
                        .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        Item item =
                itemRepository
                        .findByName(request.itemName())
                        .orElseThrow(() -> new ServiceException(ErrorCode.ITEM_NOT_FOUND));

        List<ItemHistory> unusedItems =
                itemHistoryRepository.findUnusedItems(user.getId(), item.getType());

        if (request.amount() != null) {
            unusedItems = unusedItems.stream().limit(request.amount()).collect(Collectors.toList());
        }

        itemHistoryRepository.deleteAll(unusedItems);
    }

    @Override
    public void revokeUserCoin(String username, CoinRevokeRequest request) {
        User user =
                userRepository
                        .findByName(username)
                        .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        long amountToRevoke = request.amount();
        if (amountToRevoke <= 0) {
            throw new IllegalArgumentException("회수할 씨앗은 0보다 커야 합니다.");
        }

        user.useCoin(amountToRevoke);

        CoinHistory adminRevoke =
                CoinHistory.of(user, -amountToRevoke, CoinLogType.ADMIN_REVOKE, "관리자 회수");
        coinHistoryRepository.save(adminRevoke);
    }
}
