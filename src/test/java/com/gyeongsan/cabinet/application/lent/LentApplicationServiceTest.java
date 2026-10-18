package com.gyeongsan.cabinet.application.lent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.gyeongsan.cabinet.domain.cabinet.model.Cabinet;
import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import com.gyeongsan.cabinet.domain.cabinet.model.LentType;
import com.gyeongsan.cabinet.domain.cabinet.port.out.CabinetRepositoryPort;
import com.gyeongsan.cabinet.domain.item.model.ItemType;
import com.gyeongsan.cabinet.domain.item.port.out.ItemHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.model.LentHistory;
import com.gyeongsan.cabinet.domain.lent.model.ReservationOutcome;
import com.gyeongsan.cabinet.domain.lent.port.out.AiCheckPort;
import com.gyeongsan.cabinet.domain.lent.port.out.ImageUploadPort;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.ReservationPort;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class LentApplicationServiceTest {

    @Mock private UserRepositoryPort userRepository;
    @Mock private CabinetRepositoryPort cabinetRepository;
    @Mock private LentRepositoryPort lentRepository;
    @Mock private ItemHistoryRepositoryPort itemHistoryRepository;
    @Mock private ReservationPort reservationPort;
    @Mock private AiCheckPort aiCheckPort;
    @Mock private ImageUploadPort imageUploadPort;
    @Mock private TransactionTemplate transactionTemplate;

    @InjectMocks private LentApplicationService lentApplicationService;

    private static final Long USER_ID = 1L;
    private static final Integer NEW_CABINET_NUM = 15;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(lentApplicationService, "lentTerm", 31);
        ReflectionTestUtils.setField(lentApplicationService, "extensionTerm", 3L);
    }

    private User user() {
        return User.builder()
                .id(USER_ID)
                .name("tester")
                .role(UserRole.USER)
                .isPisciner(false)
                .penaltyDays(0)
                .build();
    }

    private Cabinet availableCabinet(Integer visibleNum) {
        return Cabinet.of(
                visibleNum, CabinetStatus.AVAILABLE, LentType.PRIVATE, 1, null, 1, "A", 1, 1);
    }

    @Test
    @DisplayName("makeReservation: 기존 예약을 교체하며 성공한다")
    void makeReservation_replacesExistingReservation() {
        given(reservationPort.getUserReservation(USER_ID)).willReturn(Optional.of(12));
        given(lentRepository.findByUserIdAndEndedAtIsNull(USER_ID)).willReturn(Optional.empty());
        given(cabinetRepository.findByVisibleNumWithLock(NEW_CABINET_NUM))
                .willReturn(Optional.of(availableCabinet(NEW_CABINET_NUM)));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user()));
        given(reservationPort.reserveReplacing(NEW_CABINET_NUM, USER_ID, 15))
                .willReturn(new ReservationOutcome(ReservationOutcome.Status.RESERVED, 12));

        lentApplicationService.makeReservation(USER_ID, NEW_CABINET_NUM);

        verify(reservationPort).reserveReplacing(NEW_CABINET_NUM, USER_ID, 15);
    }

    @Test
    @DisplayName("makeReservation: 같은 번호를 다시 예약하면 400(ALREADY_RESERVED)")
    void makeReservation_sameNumber_throwsAlreadyReserved() {
        given(reservationPort.getUserReservation(USER_ID)).willReturn(Optional.of(NEW_CABINET_NUM));

        assertThatThrownBy(() -> lentApplicationService.makeReservation(USER_ID, NEW_CABINET_NUM))
                .isInstanceOf(ServiceException.class)
                .extracting(e -> ((ServiceException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_RESERVED);

        verify(reservationPort, never()).reserveReplacing(any(), any(), anyLong());
    }

    @Test
    @DisplayName("makeReservation: 다른 사람이 선점하면 400(CABINET_ALREADY_RESERVED)")
    void makeReservation_takenByOther_throwsCabinetAlreadyReserved() {
        given(reservationPort.getUserReservation(USER_ID)).willReturn(Optional.empty());
        given(lentRepository.findByUserIdAndEndedAtIsNull(USER_ID)).willReturn(Optional.empty());
        given(cabinetRepository.findByVisibleNumWithLock(NEW_CABINET_NUM))
                .willReturn(Optional.of(availableCabinet(NEW_CABINET_NUM)));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user()));
        given(reservationPort.reserveReplacing(NEW_CABINET_NUM, USER_ID, 15))
                .willReturn(new ReservationOutcome(ReservationOutcome.Status.TAKEN_BY_OTHER, null));

        assertThatThrownBy(() -> lentApplicationService.makeReservation(USER_ID, NEW_CABINET_NUM))
                .isInstanceOf(ServiceException.class)
                .extracting(e -> ((ServiceException) e).getErrorCode())
                .isEqualTo(ErrorCode.CABINET_ALREADY_RESERVED);
    }

    @Test
    @DisplayName("makeReservation: 이사권 없이 대여 중이면 400(SWAP_TICKET_NOT_FOUND)")
    void makeReservation_rentingWithoutSwapTicket_throws() {
        given(reservationPort.getUserReservation(USER_ID)).willReturn(Optional.empty());
        given(lentRepository.findByUserIdAndEndedAtIsNull(USER_ID))
                .willReturn(Optional.of(LentHistory.of(user(), availableCabinet(1), null, null)));
        given(itemHistoryRepository.findUnusedItems(USER_ID, ItemType.SWAP)).willReturn(List.of());

        assertThatThrownBy(() -> lentApplicationService.makeReservation(USER_ID, NEW_CABINET_NUM))
                .isInstanceOf(ServiceException.class)
                .extracting(e -> ((ServiceException) e).getErrorCode())
                .isEqualTo(ErrorCode.SWAP_TICKET_NOT_FOUND);
    }

    @Test
    @DisplayName("makeReservation: 사물함이 없으면 404(CABINET_NOT_FOUND)")
    void makeReservation_cabinetNotFound_throws() {
        given(reservationPort.getUserReservation(USER_ID)).willReturn(Optional.empty());
        given(lentRepository.findByUserIdAndEndedAtIsNull(USER_ID)).willReturn(Optional.empty());
        given(cabinetRepository.findByVisibleNumWithLock(NEW_CABINET_NUM))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> lentApplicationService.makeReservation(USER_ID, NEW_CABINET_NUM))
                .isInstanceOf(ServiceException.class)
                .extracting(e -> ((ServiceException) e).getErrorCode())
                .isEqualTo(ErrorCode.CABINET_NOT_FOUND);
    }

    @Test
    @DisplayName("makeReservation: AVAILABLE 상태가 아니면 400(INVALID_CABINET_STATUS)")
    void makeReservation_notAvailable_throws() {
        given(reservationPort.getUserReservation(USER_ID)).willReturn(Optional.empty());
        given(lentRepository.findByUserIdAndEndedAtIsNull(USER_ID)).willReturn(Optional.empty());
        Cabinet full =
                Cabinet.of(
                        NEW_CABINET_NUM,
                        CabinetStatus.FULL,
                        LentType.PRIVATE,
                        1,
                        null,
                        1,
                        "A",
                        1,
                        1);
        given(cabinetRepository.findByVisibleNumWithLock(NEW_CABINET_NUM))
                .willReturn(Optional.of(full));

        assertThatThrownBy(() -> lentApplicationService.makeReservation(USER_ID, NEW_CABINET_NUM))
                .isInstanceOf(ServiceException.class)
                .extracting(e -> ((ServiceException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CABINET_STATUS);
    }

    @Test
    @DisplayName("makeReservation: 유저가 없으면 404(USER_NOT_FOUND)")
    void makeReservation_userNotFound_throws() {
        given(reservationPort.getUserReservation(USER_ID)).willReturn(Optional.empty());
        given(lentRepository.findByUserIdAndEndedAtIsNull(USER_ID)).willReturn(Optional.empty());
        given(cabinetRepository.findByVisibleNumWithLock(NEW_CABINET_NUM))
                .willReturn(Optional.of(availableCabinet(NEW_CABINET_NUM)));
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> lentApplicationService.makeReservation(USER_ID, NEW_CABINET_NUM))
                .isInstanceOf(ServiceException.class)
                .extracting(e -> ((ServiceException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("makeReservation: 피시너가 일반 사물함을 예약하면 403")
    void makeReservation_piscinerRestricted_throws() {
        given(reservationPort.getUserReservation(USER_ID)).willReturn(Optional.empty());
        given(lentRepository.findByUserIdAndEndedAtIsNull(USER_ID)).willReturn(Optional.empty());
        given(cabinetRepository.findByVisibleNumWithLock(NEW_CABINET_NUM))
                .willReturn(Optional.of(availableCabinet(NEW_CABINET_NUM)));
        User pisciner =
                User.builder()
                        .id(USER_ID)
                        .name("pisciner")
                        .role(UserRole.USER)
                        .isPisciner(true)
                        .penaltyDays(0)
                        .build();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(pisciner));

        assertThatThrownBy(() -> lentApplicationService.makeReservation(USER_ID, NEW_CABINET_NUM))
                .isInstanceOf(ServiceException.class)
                .extracting(e -> ((ServiceException) e).getErrorCode())
                .isEqualTo(ErrorCode.PISCINER_CABINET_RESTRICTED);
    }

    @Test
    @DisplayName("cancelReservation: 예약이 있으면 취소하고 사물함 번호를 반환한다")
    void cancelReservation_success() {
        given(reservationPort.cancelUserReservation(USER_ID))
                .willReturn(Optional.of(NEW_CABINET_NUM));

        Integer cancelled = lentApplicationService.cancelReservation(USER_ID);

        assertThat(cancelled).isEqualTo(NEW_CABINET_NUM);
    }

    @Test
    @DisplayName("cancelReservation: 예약이 없으면 404(RESERVATION_NOT_FOUND)")
    void cancelReservation_notFound_throws() {
        given(reservationPort.cancelUserReservation(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> lentApplicationService.cancelReservation(USER_ID))
                .isInstanceOf(ServiceException.class)
                .extracting(e -> ((ServiceException) e).getErrorCode())
                .isEqualTo(ErrorCode.RESERVATION_NOT_FOUND);
    }
}
