package com.gyeongsan.cabinet.lent.service;

import com.gyeongsan.cabinet.cabinet.domain.Cabinet;
import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;
import com.gyeongsan.cabinet.cabinet.domain.LentType;
import com.gyeongsan.cabinet.cabinet.repository.CabinetRepository;
import com.gyeongsan.cabinet.item.domain.Item;
import com.gyeongsan.cabinet.item.domain.ItemHistory;
import com.gyeongsan.cabinet.item.domain.ItemType;
import com.gyeongsan.cabinet.item.repository.ItemHistoryRepository;
import com.gyeongsan.cabinet.lent.domain.LentHistory;
import com.gyeongsan.cabinet.lent.repository.LentRepository;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.domain.UserRole;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LentFacadeServiceTest {

    @InjectMocks
    private LentFacadeService lentFacadeService;

    @Mock private UserRepository userRepository;
    @Mock private CabinetRepository cabinetRepository;
    @Mock private LentRepository lentRepository;
    @Mock private ItemHistoryRepository itemHistoryRepository;

    @Test
    @DisplayName("✅ 대여 성공 - 모든 조건이 완벽할 때")
    void startLentCabinet_Success() {
        Long userId = 1L;
        Long cabinetId = 1L;

        User mockUser = User.of("testUser", "test@test.com", UserRole.USER);
        Cabinet mockCabinet = Cabinet.of(1001, CabinetStatus.AVAILABLE, LentType.PRIVATE, 1, null, 2, "A", 1, 1);
        Item mockItem = new Item("대여권", ItemType.LENT, 1000L, "설명");
        ItemHistory mockTicket = new ItemHistory(LocalDateTime.now(), null, mockUser, mockItem);

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(cabinetRepository.findByIdWithLock(cabinetId)).willReturn(Optional.of(mockCabinet));
        given(lentRepository.findByUserIdAndEndedAtIsNull(userId)).willReturn(Optional.empty());
        given(itemHistoryRepository.findUnusedItems(userId, ItemType.LENT))
                .willReturn(List.of(mockTicket));

        lentFacadeService.startLentCabinet(userId, cabinetId);

        assertEquals(CabinetStatus.FULL, mockCabinet.getStatus());
        assertNotNull(mockTicket.getUsedAt());
        verify(lentRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("❌ 대여 실패 - 대여권이 없을 때")
    void startLentCabinet_Fail_NoTicket() {
        Long userId = 1L;
        Long cabinetId = 1L;
        User mockUser = User.of("testUser", "test@test.com", UserRole.USER);
        Cabinet mockCabinet = Cabinet.of(1001, CabinetStatus.AVAILABLE, LentType.PRIVATE, 1, null, 2, "A", 1, 1);

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(cabinetRepository.findByIdWithLock(cabinetId)).willReturn(Optional.of(mockCabinet));
        given(lentRepository.findByUserIdAndEndedAtIsNull(userId)).willReturn(Optional.empty());
        given(itemHistoryRepository.findUnusedItems(userId, ItemType.LENT)).willReturn(Collections.emptyList());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            lentFacadeService.startLentCabinet(userId, cabinetId);
        });

        assertEquals("대여권(ITEM)이 부족합니다! 상점에서 구매해주세요.", exception.getMessage());
    }

    @Test
    @DisplayName("❌ 대여 실패 - 이미 빌린 사물함이 있을 때")
    void startLentCabinet_Fail_AlreadyLent() {
        Long userId = 1L;
        Long cabinetId = 1L;
        User mockUser = User.of("testUser", "test@test.com", UserRole.USER);
        Cabinet mockCabinet = Cabinet.of(1001, CabinetStatus.AVAILABLE, LentType.PRIVATE, 1, null, 2, "A", 1, 1);

        LentHistory activeLent = LentHistory.of(mockUser, mockCabinet, LocalDateTime.now(), LocalDateTime.now().plusDays(30));

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(cabinetRepository.findByIdWithLock(cabinetId)).willReturn(Optional.of(mockCabinet));
        given(lentRepository.findByUserIdAndEndedAtIsNull(userId)).willReturn(Optional.of(activeLent));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            lentFacadeService.startLentCabinet(userId, cabinetId);
        });

        assertEquals("이미 대여 중인 사물함이 있습니다.", exception.getMessage());
    }
}
