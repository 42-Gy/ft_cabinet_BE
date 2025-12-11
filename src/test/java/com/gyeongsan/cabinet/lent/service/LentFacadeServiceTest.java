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

@ExtendWith(MockitoExtension.class) // DB 연결 없이 Mockito 환경에서 테스트 실행
class LentFacadeServiceTest {

    @InjectMocks
    private LentFacadeService lentFacadeService; // 테스트할 대상 (서비스)

    // 가짜(Mock) 저장소들
    @Mock private UserRepository userRepository;
    @Mock private CabinetRepository cabinetRepository;
    @Mock private LentRepository lentRepository;
    @Mock private ItemHistoryRepository itemHistoryRepository;

    @Test
    @DisplayName("✅ 대여 성공 - 모든 조건이 완벽할 때")
    void startLentCabinet_Success() {
        // Given (상황 설정)
        Long userId = 1L;
        Long cabinetId = 1L;

        // 가짜 데이터 생성
        User mockUser = User.of("testUser", "test@test.com", UserRole.USER);
        Cabinet mockCabinet = Cabinet.of(1001, CabinetStatus.AVAILABLE, LentType.PRIVATE, 1, null, 2, "A", 1, 1);
        Item mockItem = new Item("대여권", ItemType.LENT, 1000L, "설명");
        ItemHistory mockTicket = new ItemHistory(LocalDateTime.now(), null, mockUser, mockItem);

        // Mocking (레포지토리가 이렇게 동작한다고 가정)
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(cabinetRepository.findByIdWithLock(cabinetId)).willReturn(Optional.of(mockCabinet));
        given(lentRepository.findByUserIdAndEndedAtIsNull(userId)).willReturn(Optional.empty()); // 중복 대여 없음
        given(itemHistoryRepository.findUnusedItems(userId, ItemType.LENT))
                .willReturn(List.of(mockTicket)); // 대여권 있음

        // When (실행)
        lentFacadeService.startLentCabinet(userId, cabinetId);

        // Then (검증)
        // 1. 사물함 상태가 FULL로 변했는가?
        assertEquals(CabinetStatus.FULL, mockCabinet.getStatus());
        // 2. 대여권이 사용 처리되었는가? (usedAt이 null이 아님)
        assertNotNull(mockTicket.getUsedAt());
        // 3. 대여 기록 저장 메서드가 호출되었는가?
        verify(lentRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("❌ 대여 실패 - 대여권이 없을 때")
    void startLentCabinet_Fail_NoTicket() {
        // Given
        Long userId = 1L;
        Long cabinetId = 1L;
        User mockUser = User.of("testUser", "test@test.com", UserRole.USER);
        Cabinet mockCabinet = Cabinet.of(1001, CabinetStatus.AVAILABLE, LentType.PRIVATE, 1, null, 2, "A", 1, 1);

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(cabinetRepository.findByIdWithLock(cabinetId)).willReturn(Optional.of(mockCabinet));
        given(lentRepository.findByUserIdAndEndedAtIsNull(userId)).willReturn(Optional.empty());

        // 대여권 없음 (빈 리스트 반환 설정)
        given(itemHistoryRepository.findUnusedItems(userId, ItemType.LENT)).willReturn(Collections.emptyList());

        // When & Then (예외 발생 검증)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            lentFacadeService.startLentCabinet(userId, cabinetId);
        });

        assertEquals("대여권(ITEM)이 부족합니다! 상점에서 구매해주세요.", exception.getMessage());
    }

    @Test
    @DisplayName("❌ 대여 실패 - 이미 빌린 사물함이 있을 때")
    void startLentCabinet_Fail_AlreadyLent() {
        // Given
        Long userId = 1L;
        Long cabinetId = 1L;
        User mockUser = User.of("testUser", "test@test.com", UserRole.USER);
        Cabinet mockCabinet = Cabinet.of(1001, CabinetStatus.AVAILABLE, LentType.PRIVATE, 1, null, 2, "A", 1, 1);

        // 이미 대여 중인 기록이 있다고 가정
        LentHistory activeLent = LentHistory.of(mockUser, mockCabinet, LocalDateTime.now(), LocalDateTime.now().plusDays(30));

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(cabinetRepository.findByIdWithLock(cabinetId)).willReturn(Optional.of(mockCabinet));

        // 중복 대여 확인됨 (Optional에 값이 있음)
        given(lentRepository.findByUserIdAndEndedAtIsNull(userId)).willReturn(Optional.of(activeLent));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            lentFacadeService.startLentCabinet(userId, cabinetId);
        });

        assertEquals("이미 대여 중인 사물함이 있습니다.", exception.getMessage());
    }
}