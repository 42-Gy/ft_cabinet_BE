package com.gyeongsan.cabinet.domain.cabinet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** 사물함 목록의 daysRemaining 이 /me, 반납 응답과 같은 달력 기준인지 확인한다. */
class CabinetListRemainingDaysTest {

    private List<CabinetListResponseDto> listWithLentExpiringAt(LocalDateTime expiredAt) {
        CabinetRepositoryPort cabinetRepository = mock(CabinetRepositoryPort.class);
        LentRepositoryPort lentRepository = mock(LentRepositoryPort.class);
        CabinetDomainService service =
                new CabinetDomainService(
                        cabinetRepository, lentRepository, mock(ReservationPort.class));

        Cabinet cabinet =
                Cabinet.of(101, CabinetStatus.FULL, LentType.PRIVATE, 1, null, 1, "A", 1, 1);
        ReflectionTestUtils.setField(cabinet, "id", 101L);
        User user = User.of("intra09", "intra09@example.com", null, UserRole.USER);
        LentHistory lent = LentHistory.of(user, cabinet, expiredAt.minusDays(31), expiredAt);

        when(cabinetRepository.findAllByFloor(1)).thenReturn(List.of(cabinet));
        when(lentRepository.findAllActiveLentByCabinetIds(any())).thenReturn(List.of(lent));
        return service.getCabinetList(1, null);
    }

    @Test
    @DisplayName("만료 시각이 24시간 미만으로 남았어도 날짜가 내일이면 1 이다 (이전에는 0 으로 버림됐다)")
    void tomorrowIsOneEvenWithinTwentyFourHours() {
        // 만료일 내일 00:01: 지금이 언제든 24시간을 넘지 않을 수 있지만 달력상으로는 하루 남았다.
        List<CabinetListResponseDto> list =
                listWithLentExpiringAt(LocalDate.now().plusDays(1).atTime(0, 1));

        assertThat(list.get(0).getDaysRemaining()).isEqualTo(1);
    }

    @Test
    @DisplayName("만료일 당일은 시각과 상관없이 0, 지난 날은 음수다")
    void todayIsZeroAndPastIsNegative() {
        assertThat(listWithLentExpiringAt(LocalDate.now().atTime(23, 59)).get(0).getDaysRemaining())
                .isZero();
        assertThat(
                        listWithLentExpiringAt(LocalDate.now().minusDays(2).atTime(12, 0))
                                .get(0)
                                .getDaysRemaining())
                .isEqualTo(-2);
    }
}
