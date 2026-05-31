package com.gyeongsan.cabinet.domain.watermelon.service;

import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.domain.watermelon.domain.*;
import com.gyeongsan.cabinet.domain.watermelon.port.out.WatermelonEventLogRepositoryPort;
import com.gyeongsan.cabinet.domain.watermelon.port.out.WatermelonRepositoryPort;
import com.gyeongsan.cabinet.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatermelonEventServiceTest {

    @Mock
    private WatermelonRepositoryPort watermelonRepository;

    @Mock
    private WatermelonEventLogRepositoryPort logRepository;

    @Mock
    private UserRepositoryPort userRepository;

    @InjectMocks
    private WatermelonEventService watermelonEventService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = mock(User.class);
        lenient().when(testUser.getId()).thenReturn(1L);
    }

    @Test
    @DisplayName("코인이 부족할 경우 아이템 구매가 실패한다")
    void buyItem_insufficientCoins() {
        given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(testUser));
        given(watermelonRepository.findByUserId(1L)).willReturn(Optional.of(Watermelon.createNew(1L)));
        doThrow(new IllegalArgumentException("씨앗이 부족합니다!")).when(testUser).useCoin(anyLong());

        assertThrows(IllegalArgumentException.class, () ->
                watermelonEventService.buyItem(1L, WatermelonItem.PREMIUM_FERTILIZER, 1)
        );
    }

    @Test
    @DisplayName("코인이 부족할 경우 강화가 실패한다")
    void enhance_insufficientCoins() {
        given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(testUser));
        given(watermelonRepository.findByUserId(1L)).willReturn(Optional.of(Watermelon.createNew(1L)));
        doThrow(new IllegalArgumentException("씨앗이 부족합니다!")).when(testUser).useCoin(anyLong());

        assertThrows(IllegalArgumentException.class, () ->
                watermelonEventService.enhance(1L, false, false, false, false)
        );
    }

    @Test
    @DisplayName("비료를 중복 사용할 경우 예외가 발생한다")
    void enhance_dualFertilizersBlocked() {
        given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(testUser));
        given(watermelonRepository.findByUserId(1L)).willReturn(Optional.of(Watermelon.createNew(1L)));

        assertThrows(IllegalArgumentException.class, () ->
                watermelonEventService.enhance(1L, true, true, false, false)
        );
    }

    @Test
    @DisplayName("7강 이상에서 위험한 비료를 사용할 경우 예외가 발생한다")
    void enhance_dangerousFertilizerLevelLimit() {
        Watermelon level7Wm = Watermelon.builder()
                .userId(1L)
                .currentLevel(7)
                .dangerousFertilizerCount(5)
                .build();

        given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(testUser));
        given(watermelonRepository.findByUserId(1L)).willReturn(Optional.of(level7Wm));

        assertThrows(IllegalArgumentException.class, () ->
                watermelonEventService.enhance(1L, false, true, false, false)
        );
    }

    @Test
    @DisplayName("하락 방지권이 있고 사용 설정된 경우 DROP 결과가 MAINTAIN으로 보정되고 방지권 수량이 1개 차감된다")
    void applyEnhancement_dropProtectionSuccess() {
        Watermelon wm = Watermelon.builder()
                .userId(1L)
                .currentLevel(5)
                .dropProtectionCount(2)
                .build();

        wm.applyEnhancement(EnhancementResult.DROP, true, false);

        assertEquals(5, wm.getCurrentLevel());
        assertEquals(1, wm.getDropProtectionCount());
        assertEquals(1, wm.getTotalAttempts());
        assertEquals(1, wm.getTotalMaintains());
    }

    @Test
    @DisplayName("파괴 방지권이 있고 사용 설정된 경우 DESTROY 결과가 무효화되며 현재 레벨에서 2감소 처리되고 방지권 수량이 1개 차감된다")
    void applyEnhancement_destroyProtectionSuccess() {
        Watermelon wm = Watermelon.builder()
                .userId(1L)
                .currentLevel(5)
                .destroyProtectionCount(3)
                .build();

        wm.applyEnhancement(EnhancementResult.DESTROY, false, true);

        assertEquals(3, wm.getCurrentLevel());
        assertEquals(2, wm.getDestroyProtectionCount());
        assertEquals(1, wm.getTotalAttempts());
        assertEquals(1, wm.getTotalMaintains());
    }

    @Test
    @DisplayName("방지권을 사용 설정하였으나 보유량이 없으면 예외가 발생한다")
    void applyEnhancement_noTicketNoProtection() {
        Watermelon wm = Watermelon.builder()
                .userId(1L)
                .currentLevel(5)
                .dropProtectionCount(0)
                .destroyProtectionCount(0)
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                wm.applyEnhancement(EnhancementResult.DROP, true, false)
        );
    }

    @Test
    @DisplayName("성공 결과가 나오더라도 방지권을 사용 설정했다면 방지권 수량이 소모된다")
    void applyEnhancement_alwaysConsumeTicketsOnSuccess() {
        Watermelon wm = Watermelon.builder()
                .userId(1L)
                .currentLevel(5)
                .dropProtectionCount(2)
                .destroyProtectionCount(2)
                .build();

        wm.applyEnhancement(EnhancementResult.SUCCESS, true, true);

        assertEquals(6, wm.getCurrentLevel());
        assertEquals(1, wm.getDropProtectionCount());
        assertEquals(1, wm.getDestroyProtectionCount());
    }
}
