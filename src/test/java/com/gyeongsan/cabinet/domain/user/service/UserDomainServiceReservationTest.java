package com.gyeongsan.cabinet.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gyeongsan.cabinet.adapter.in.web.user.dto.MyProfileResponseDto;
import com.gyeongsan.cabinet.domain.coin.port.out.CoinHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.item.port.out.ItemHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.ReservationPort;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.domain.user.port.out.AttendanceRepositoryPort;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UserDomainServiceReservationTest {

    private static final Long USER_ID = 9L;

    private ReservationPort reservationPort;
    private UserDomainService service;

    @BeforeEach
    void setUp() {
        UserRepositoryPort userRepository = mock(UserRepositoryPort.class);
        reservationPort = mock(ReservationPort.class);
        service =
                new UserDomainService(
                        userRepository,
                        mock(LentRepositoryPort.class),
                        mock(ItemHistoryRepositoryPort.class),
                        mock(AttendanceRepositoryPort.class),
                        mock(CoinHistoryRepositoryPort.class),
                        reservationPort);

        User user = User.of("intra09", "intra09@example.com", null, UserRole.USER);
        ReflectionTestUtils.setField(user, "id", USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("예약이 있으면 /me 에 예약한 사물함 번호와 남은 시간이 담긴다")
    void profile_includesReservation() {
        when(reservationPort.getUserReservation(USER_ID)).thenReturn(Optional.of(201));
        when(reservationPort.getUserReservationTtlSeconds(USER_ID)).thenReturn(Optional.of(845L));

        MyProfileResponseDto profile = service.getMyProfile(USER_ID);

        assertThat(profile.getReservedVisibleNum()).isEqualTo(201);
        assertThat(profile.getReservationRemainingSeconds()).isEqualTo(845L);
    }

    @Test
    @DisplayName("예약이 없으면 두 필드는 null 이다")
    void profile_withoutReservation() {
        when(reservationPort.getUserReservation(USER_ID)).thenReturn(Optional.empty());
        when(reservationPort.getUserReservationTtlSeconds(USER_ID)).thenReturn(Optional.empty());

        MyProfileResponseDto profile = service.getMyProfile(USER_ID);

        assertThat(profile.getReservedVisibleNum()).isNull();
        assertThat(profile.getReservationRemainingSeconds()).isNull();
    }

    @Test
    @DisplayName("두 값을 읽는 사이 예약이 만료되면 예약이 없는 것으로 본다")
    void profile_reservationExpiredBetweenReads() {
        when(reservationPort.getUserReservation(USER_ID)).thenReturn(Optional.of(201));
        when(reservationPort.getUserReservationTtlSeconds(USER_ID)).thenReturn(Optional.empty());

        MyProfileResponseDto profile = service.getMyProfile(USER_ID);

        assertThat(profile.getReservedVisibleNum()).isNull();
        assertThat(profile.getReservationRemainingSeconds()).isNull();
    }

    @Test
    @DisplayName("예약 조회가 실패해도 프로필 전체는 정상으로 내려간다")
    void profile_survivesReservationLookupFailure() {
        when(reservationPort.getUserReservation(USER_ID))
                .thenThrow(new IllegalStateException("redis down"));

        MyProfileResponseDto profile = service.getMyProfile(USER_ID);

        assertThat(profile.getName()).isEqualTo("intra09");
        assertThat(profile.getReservedVisibleNum()).isNull();
    }
}
