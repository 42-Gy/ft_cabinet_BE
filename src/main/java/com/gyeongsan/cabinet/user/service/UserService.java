package com.gyeongsan.cabinet.user.service;

import com.gyeongsan.cabinet.cabinet.domain.Cabinet;
import com.gyeongsan.cabinet.item.domain.ItemHistory;
import com.gyeongsan.cabinet.item.repository.ItemHistoryRepository;
import com.gyeongsan.cabinet.lent.domain.LentHistory;
import com.gyeongsan.cabinet.lent.repository.LentRepository;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.dto.MyProfileResponseDto;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final LentRepository lentRepository;
    private final ItemHistoryRepository itemHistoryRepository;

    public MyProfileResponseDto getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        LentHistory activeLent = lentRepository.findByUserIdAndEndedAtIsNull(userId).orElse(null);

        List<ItemHistory> myItems = itemHistoryRepository.findAllByUserIdAndUsedAtIsNull(userId);

        List<MyProfileResponseDto.MyItemDto> itemDtos = myItems.stream()
                .map(item -> MyProfileResponseDto.MyItemDto.builder()
                        .itemHistoryId(item.getId())
                        .itemName(item.getItem().getName())
                        .itemType(item.getItem().getType().name())
                        .purchaseAt(item.getPurchaseAt())
                        .build())
                .collect(Collectors.toList());

        Long cabinetId = null;
        Integer visibleNum = null;
        String section = null;
        String lentStartedAt = null;

        if (activeLent != null) {
            Cabinet cabinet = activeLent.getCabinet();
            cabinetId = cabinet.getId();
            visibleNum = cabinet.getVisibleNum();
            section = cabinet.getSection();
            lentStartedAt = activeLent.getStartedAt().toString();
        }

        return MyProfileResponseDto.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .coin(user.getCoin())
                .penaltyDays(user.getPenaltyDays())
                .lentCabinetId(cabinetId)
                .visibleNum(visibleNum)
                .section(section)
                .lentStartedAt(lentStartedAt)
                .myItems(itemDtos)
                .build();
    }
}
