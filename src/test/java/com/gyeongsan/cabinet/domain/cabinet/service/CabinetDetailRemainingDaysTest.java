package com.gyeongsan.cabinet.domain.cabinet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gyeongsan.cabinet.adapter.in.web.cabinet.dto.CabinetDetailResponseDto;
import com.gyeongsan.cabinet.adapter.in.web.cabinet.dto.CabinetListResponseDto;
import com.gyeongsan.cabinet.domain.cabinet.model.Cabinet;
import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import com.gyeongsan.cabinet.domain.cabinet.model.LentType;
import com.gyeongsan.cabinet.domain.cabinet.port.out.CabinetRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.model.LentHistory;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.ReservationPort;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** 사물함 상세의 daysRemaining 이 목록과 같은 달력 기준인지, 대여 중이 아닐 때는 null 인지 확인한다. */
class CabinetDetailRemainingDaysTest {

    private static final long CABINET_ID = 101L;

    private CabinetRepositoryPort cabinetRepository;
    private LentRepositoryPort lentRepository;
    private CabinetDomainService service;
    private Cabinet cabinet;
    private User user;

    @BeforeEach
    void setUp() {
        cabinetRepository = mock(CabinetRepositoryPort.class);
        lentRepository = mock(LentRepositoryPort.class);
        service =
                new CabinetDomainService(
                        cabinetRepository, lentRepository, mock(ReservationPort.class));

        cabinet = Cabinet.of(101, CabinetStatus.FULL, LentType.PRIVATE, 1, null, 1, "A", 1, 1);
        ReflectionTestUtils.setField(cabinet, "id", CABINET_ID);
        user = User.of("intra09", "intra09@example.com", null, UserRole.USER);

        when(cabinetRepository.findById(CABINET_ID)).thenReturn(Optional.of(cabinet));
        when(lentRepository.findTopByCabinetIdAndEndedAtIsNotNullOrderByEndedAtDesc(CABINET_ID))
                .thenReturn(Optional.empty());
    }

    private LentHistory lentExpiringAt(LocalDateTime expiredAt) {
        LentHistory lent = LentHistory.of(user, cabinet, expiredAt.minusDays(31), expiredAt);
        when(lentRepository.findByCabinetIdAndEndedAtIsNull(CABINET_ID))
                .thenReturn(Optional.of(lent));
        return lent;
    }

    @Test
    @DisplayName("대여 중이면 만료일까지 남은 달력 일수를 주고, 기존 lentExpiredAt 도 그대로다")
    void activeLent() {
        LocalDateTime expiredAt = LocalDate.now().plusDays(5).atTime(14, 23);
        lentExpiringAt(expiredAt);

        CabinetDetailResponseDto detail = service.getCabinetDetail(CABINET_ID, null);

        assertThat(detail.getDaysRemaining()).isEqualTo(5L);
        assertThat(detail.getLentExpiredAt()).isEqualTo(expiredAt);
    }

    @Test
    @DisplayName("만료 시각이 24시간 미만으로 남아도 날짜가 내일이면 1, 당일은 0, 지난 날은 음수다")
    void calendarBoundaries() {
        lentExpiringAt(LocalDate.now().plusDays(1).atTime(0, 1));
        assertThat(service.getCabinetDetail(CABINET_ID, null).getDaysRemaining()).isEqualTo(1L);

        lentExpiringAt(LocalDate.now().atTime(23, 59));
        assertThat(service.getCabinetDetail(CABINET_ID, null).getDaysRemaining()).isZero();

        lentExpiringAt(LocalDate.now().minusDays(2).atTime(12, 0));
        assertThat(service.getCabinetDetail(CABINET_ID, null).getDaysRemaining()).isEqualTo(-2L);
    }

    @Test
    @DisplayName("같은 대여는 목록과 상세가 같은 남은 일수를 준다")
    void sameAsList() {
        LentHistory lent = lentExpiringAt(LocalDate.now().plusDays(3).atTime(9, 0));
        when(cabinetRepository.findAllByFloor(1)).thenReturn(List.of(cabinet));
        when(lentRepository.findAllActiveLentByCabinetIds(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(lent));

        CabinetListResponseDto fromList = service.getCabinetList(1, null).get(0);
        CabinetDetailResponseDto fromDetail = service.getCabinetDetail(CABINET_ID, null);

        assertThat(fromDetail.getDaysRemaining()).isEqualTo(fromList.getDaysRemaining());
    }

    @Test
    @DisplayName("대여 중이 아니면 null 이다(0 은 '오늘 만료'와 구분되지 않는다)")
    void noActiveLent() {
        when(lentRepository.findByCabinetIdAndEndedAtIsNull(CABINET_ID))
                .thenReturn(Optional.empty());

        CabinetDetailResponseDto detail = service.getCabinetDetail(CABINET_ID, null);

        assertThat(detail.getDaysRemaining()).isNull();
        assertThat(detail.getLentExpiredAt()).isNull();
    }
}
