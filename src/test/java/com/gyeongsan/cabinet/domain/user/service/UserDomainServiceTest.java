package com.gyeongsan.cabinet.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.gyeongsan.cabinet.adapter.in.web.user.dto.MyProfileResponseDto;
import com.gyeongsan.cabinet.domain.cabinet.model.Cabinet;
import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import com.gyeongsan.cabinet.domain.cabinet.model.LentType;
import com.gyeongsan.cabinet.domain.coin.port.out.CoinHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.item.port.out.ItemHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.model.LentHistory;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.ReservationPort;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.domain.user.port.out.AttendanceRepositoryPort;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserDomainServiceTest {

    @Mock private UserRepositoryPort userRepository;
    @Mock private LentRepositoryPort lentRepository;
    @Mock private ItemHistoryRepositoryPort itemHistoryRepository;
    @Mock private AttendanceRepositoryPort attendanceRepository;
    @Mock private CoinHistoryRepositoryPort coinHistoryRepository;
    @Mock private ReservationPort reservationPort;

    @InjectMocks private UserDomainService userDomainService;

    private static final Long USER_ID = 1L;

    private void stubCommonProfileLookups() {
        User user = User.builder().id(USER_ID).name("tester").role(UserRole.USER).build();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(lentRepository.findByUserIdAndEndedAtIsNull(USER_ID)).willReturn(Optional.empty());
        given(lentRepository.existsByUserId(USER_ID)).willReturn(true);
        given(itemHistoryRepository.findAllByUserIdAndUsedAtIsNull(USER_ID)).willReturn(List.of());
        given(coinHistoryRepository.findAllByUserIdOrderByCreatedAtDesc(USER_ID))
                .willReturn(List.of());
        given(itemHistoryRepository.findAllByUserIdOrderByPurchaseAtDesc(USER_ID))
                .willReturn(List.of());
    }

    private Cabinet cabinet() {
        return Cabinet.of(10, CabinetStatus.FULL, LentType.PRIVATE, 1, null, 1, "A", 1, 1);
    }

    @Test
    @DisplayName("getMyProfile: 예약이 있으면 번호와 남은 시간을 함께 내려준다")
    void getMyProfile_withReservation() {
        stubCommonProfileLookups();
        given(reservationPort.getUserReservation(USER_ID)).willReturn(Optional.of(15));
        given(reservationPort.getUserReservationTtlSeconds(USER_ID)).willReturn(Optional.of(600L));

        MyProfileResponseDto dto = userDomainService.getMyProfile(USER_ID);

        assertThat(dto.getReservedVisibleNum()).isEqualTo(15);
        assertThat(dto.getReservationRemainingSeconds()).isEqualTo(600L);
    }

    @Test
    @DisplayName("getMyProfile: 예약이 없으면 두 필드 모두 null이다")
    void getMyProfile_withoutReservation_bothNull() {
        stubCommonProfileLookups();
        given(reservationPort.getUserReservation(USER_ID)).willReturn(Optional.empty());

        MyProfileResponseDto dto = userDomainService.getMyProfile(USER_ID);

        assertThat(dto.getReservedVisibleNum()).isNull();
        assertThat(dto.getReservationRemainingSeconds()).isNull();
    }

    @Test
    @DisplayName("getMyProfile: Redis 장애 시 두 필드 모두 null이고 예외가 전파되지 않는다")
    void getMyProfile_redisFailure_bothNull() {
        stubCommonProfileLookups();
        given(reservationPort.getUserReservation(USER_ID))
                .willThrow(new RuntimeException("redis down"));

        MyProfileResponseDto dto = userDomainService.getMyProfile(USER_ID);

        assertThat(dto.getReservedVisibleNum()).isNull();
        assertThat(dto.getReservationRemainingSeconds()).isNull();
    }

    @Test
    @DisplayName("getMyProfile: 대여 이력이 없으면 isFirstRental=true")
    void getMyProfile_noLentHistory_firstRentalTrue() {
        stubCommonProfileLookups();
        given(lentRepository.existsByUserId(USER_ID)).willReturn(false);
        given(reservationPort.getUserReservation(USER_ID)).willReturn(Optional.empty());

        MyProfileResponseDto dto = userDomainService.getMyProfile(USER_ID);

        assertThat(dto.getIsFirstRental()).isTrue();
    }

    @Test
    @DisplayName("getMyProfile: 현재 대여 중이면 isFirstRental=false")
    void getMyProfile_currentlyRenting_firstRentalFalse() {
        stubCommonProfileLookups();
        LentHistory activeLent =
                LentHistory.of(
                        User.builder().id(USER_ID).name("tester").role(UserRole.USER).build(),
                        cabinet(),
                        LocalDateTime.now(),
                        LocalDateTime.now().plusDays(30));
        given(lentRepository.findByUserIdAndEndedAtIsNull(USER_ID))
                .willReturn(Optional.of(activeLent));
        given(lentRepository.existsByUserId(USER_ID)).willReturn(true);
        given(
                        lentRepository.findTopByCabinetIdAndEndedAtIsNotNullOrderByEndedAtDesc(
                                cabinet().getId()))
                .willReturn(Optional.empty());
        given(reservationPort.getUserReservation(USER_ID)).willReturn(Optional.empty());

        MyProfileResponseDto dto = userDomainService.getMyProfile(USER_ID);

        assertThat(dto.getIsFirstRental()).isFalse();
    }

    @Test
    @DisplayName("getMyProfile: 대여 후 반납을 끝낸 사용자는 isFirstRental=false")
    void getMyProfile_returnedBefore_firstRentalFalse() {
        stubCommonProfileLookups();
        given(lentRepository.findByUserIdAndEndedAtIsNull(USER_ID)).willReturn(Optional.empty());
        given(lentRepository.existsByUserId(USER_ID)).willReturn(true);
        given(reservationPort.getUserReservation(USER_ID)).willReturn(Optional.empty());

        MyProfileResponseDto dto = userDomainService.getMyProfile(USER_ID);

        assertThat(dto.getIsFirstRental()).isFalse();
    }

    @Test
    @DisplayName("getMyProfile: 이력이 여러 건이어도 isFirstRental=false")
    void getMyProfile_multipleHistoryRows_firstRentalFalse() {
        stubCommonProfileLookups();
        given(lentRepository.findByUserIdAndEndedAtIsNull(USER_ID)).willReturn(Optional.empty());
        given(lentRepository.existsByUserId(USER_ID)).willReturn(true);
        given(reservationPort.getUserReservation(USER_ID)).willReturn(Optional.empty());

        MyProfileResponseDto dto = userDomainService.getMyProfile(USER_ID);

        assertThat(dto.getIsFirstRental()).isFalse();
    }
}
