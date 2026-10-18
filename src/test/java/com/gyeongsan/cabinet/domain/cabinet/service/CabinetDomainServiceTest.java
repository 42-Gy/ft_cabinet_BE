package com.gyeongsan.cabinet.domain.cabinet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.gyeongsan.cabinet.adapter.in.web.cabinet.dto.CabinetDetailResponseDto;
import com.gyeongsan.cabinet.adapter.in.web.cabinet.dto.CabinetListResponseDto;
import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import com.gyeongsan.cabinet.domain.cabinet.model.Cabinet;
import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import com.gyeongsan.cabinet.domain.cabinet.model.LentType;
import com.gyeongsan.cabinet.domain.cabinet.port.out.CabinetRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.ReservationPort;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CabinetDomainServiceTest {

    @Mock private CabinetRepositoryPort cabinetRepository;
    @Mock private LentRepositoryPort lentRepository;
    @Mock private ReservationPort reservationPort;

    @InjectMocks private CabinetDomainService cabinetDomainService;

    private static final Integer FLOOR = 1;

    private Cabinet cabinet(Integer visibleNum, CabinetStatus status) {
        return Cabinet.of(visibleNum, status, LentType.PRIVATE, 1, null, FLOOR, "A", 1, 1);
    }

    private UserPrincipal principal(Long userId) {
        User user = User.builder().id(userId).name("u" + userId).role(UserRole.USER).build();
        return new UserPrincipal(user, Map.of());
    }

    @Test
    @DisplayName("getCabinetList: AVAILABLE 사물함만 예약 조회하고 내 예약 여부를 표시한다")
    void getCabinetList_marksReservedAndMine() {
        Cabinet available = cabinet(10, CabinetStatus.AVAILABLE);
        Cabinet full = cabinet(11, CabinetStatus.FULL);
        given(cabinetRepository.findAllByFloor(FLOOR)).willReturn(List.of(available, full));
        given(lentRepository.findAllActiveLentByCabinetIds(anyList())).willReturn(List.of());
        given(reservationPort.getReservedUserIds(Set.of(10))).willReturn(Map.of(10, 1L));

        List<CabinetListResponseDto> result =
                cabinetDomainService.getCabinetList(FLOOR, principal(1L));

        CabinetListResponseDto availableDto =
                result.stream().filter(d -> d.getVisibleNum().equals(10)).findFirst().orElseThrow();
        CabinetListResponseDto fullDto =
                result.stream().filter(d -> d.getVisibleNum().equals(11)).findFirst().orElseThrow();

        assertThat(availableDto.getIsReserved()).isTrue();
        assertThat(availableDto.getIsReservedByMe()).isTrue();
        assertThat(fullDto.getIsReserved()).isFalse();
        assertThat(fullDto.getIsReservedByMe()).isFalse();
    }

    @Test
    @DisplayName("getCabinetList: AVAILABLE 사물함이 없으면 Redis를 호출하지 않는다")
    void getCabinetList_noAvailable_skipsRedisCall() {
        Cabinet full = cabinet(11, CabinetStatus.FULL);
        given(cabinetRepository.findAllByFloor(FLOOR)).willReturn(List.of(full));
        given(lentRepository.findAllActiveLentByCabinetIds(anyList())).willReturn(List.of());

        List<CabinetListResponseDto> result =
                cabinetDomainService.getCabinetList(FLOOR, principal(1L));

        assertThat(result.get(0).getIsReserved()).isFalse();
        verify(reservationPort, never()).getReservedUserIds(any());
    }

    @Test
    @DisplayName("getCabinetList: Redis 장애 시 예약 필드는 false로 내려간다")
    void getCabinetList_redisFailure_fallsBackToFalse() {
        Cabinet available = cabinet(10, CabinetStatus.AVAILABLE);
        given(cabinetRepository.findAllByFloor(FLOOR)).willReturn(List.of(available));
        given(lentRepository.findAllActiveLentByCabinetIds(anyList())).willReturn(List.of());
        given(reservationPort.getReservedUserIds(Set.of(10)))
                .willThrow(new RuntimeException("redis down"));

        List<CabinetListResponseDto> result =
                cabinetDomainService.getCabinetList(FLOOR, principal(1L));

        assertThat(result.get(0).getIsReserved()).isFalse();
        assertThat(result.get(0).getIsReservedByMe()).isFalse();
    }

    @Test
    @DisplayName("getCabinetDetail: 내가 예약했으면 남은 시간과 함께 true를 반환한다")
    void getCabinetDetail_mine() {
        Cabinet cabinet = cabinet(10, CabinetStatus.AVAILABLE);
        given(cabinetRepository.findById(1L)).willReturn(Optional.of(cabinet));
        given(lentRepository.findByCabinetIdAndEndedAtIsNull(1L)).willReturn(Optional.empty());
        given(lentRepository.findTopByCabinetIdAndEndedAtIsNotNullOrderByEndedAtDesc(1L))
                .willReturn(Optional.empty());
        given(reservationPort.getReservedUserId(10)).willReturn(Optional.of(1L));
        given(reservationPort.getCabinetReservationTtlSeconds(10)).willReturn(Optional.of(300L));

        CabinetDetailResponseDto dto = cabinetDomainService.getCabinetDetail(1L, principal(1L));

        assertThat(dto.getIsReserved()).isTrue();
        assertThat(dto.getIsReservedByMe()).isTrue();
        assertThat(dto.getReservationRemainingSeconds()).isEqualTo(300L);
    }

    @Test
    @DisplayName("getCabinetDetail: 타인이 예약했으면 isReservedByMe는 false")
    void getCabinetDetail_other() {
        Cabinet cabinet = cabinet(10, CabinetStatus.AVAILABLE);
        given(cabinetRepository.findById(1L)).willReturn(Optional.of(cabinet));
        given(lentRepository.findByCabinetIdAndEndedAtIsNull(1L)).willReturn(Optional.empty());
        given(lentRepository.findTopByCabinetIdAndEndedAtIsNotNullOrderByEndedAtDesc(1L))
                .willReturn(Optional.empty());
        given(reservationPort.getReservedUserId(10)).willReturn(Optional.of(2L));
        given(reservationPort.getCabinetReservationTtlSeconds(10)).willReturn(Optional.of(120L));

        CabinetDetailResponseDto dto = cabinetDomainService.getCabinetDetail(1L, principal(1L));

        assertThat(dto.getIsReserved()).isTrue();
        assertThat(dto.getIsReservedByMe()).isFalse();
    }

    @Test
    @DisplayName("getCabinetDetail: Redis 장애 시 false/null로 응답한다 (500을 내지 않는다)")
    void getCabinetDetail_redisFailure_fallsBack() {
        Cabinet cabinet = cabinet(10, CabinetStatus.AVAILABLE);
        given(cabinetRepository.findById(1L)).willReturn(Optional.of(cabinet));
        given(lentRepository.findByCabinetIdAndEndedAtIsNull(1L)).willReturn(Optional.empty());
        given(lentRepository.findTopByCabinetIdAndEndedAtIsNotNullOrderByEndedAtDesc(1L))
                .willReturn(Optional.empty());
        given(reservationPort.getReservedUserId(10)).willThrow(new RuntimeException("redis down"));

        CabinetDetailResponseDto dto = cabinetDomainService.getCabinetDetail(1L, principal(1L));

        assertThat(dto.getIsReserved()).isFalse();
        assertThat(dto.getIsReservedByMe()).isFalse();
        assertThat(dto.getReservationRemainingSeconds()).isNull();
    }
}
