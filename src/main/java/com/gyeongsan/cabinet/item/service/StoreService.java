package com.gyeongsan.cabinet.item.service;

import com.gyeongsan.cabinet.item.domain.Item;
import com.gyeongsan.cabinet.item.domain.ItemHistory;
import com.gyeongsan.cabinet.item.repository.ItemHistoryRepository;
import com.gyeongsan.cabinet.item.repository.ItemRepository;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Log4j2
public class StoreService {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ItemHistoryRepository itemHistoryRepository;

    /**
     * 아이템 구매 (코인 차감 -> 아이템 지급)
     */
    @Transactional
    public void buyItem(Long userId, Long itemId) {
        // 1. 유저와 아이템 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 없습니다."));

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이템입니다."));

        log.info("구매 요청 - 유저: {}, 아이템: {}, 가격: {}", user.getName(), item.getName(), item.getPrice());

        // 2. [검증 & 결제] 유저의 코인 차감 (돈 없으면 여기서 에러 터짐!)
        user.useCoin(item.getPrice());

        // 3. [지급] 인벤토리에 아이템 추가
        ItemHistory history = new ItemHistory(LocalDateTime.now(), null, user, item);
        itemHistoryRepository.save(history);

        log.info("구매 성공! 남은 코인: {}", user.getCoin());
    }
}