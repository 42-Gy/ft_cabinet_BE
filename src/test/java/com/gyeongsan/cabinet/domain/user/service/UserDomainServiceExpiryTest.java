package com.gyeongsan.cabinet.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UserDomainServiceExpiryTest {

    private static final Long USER_ID = 9L;

    private LentRepositoryPort lentRepository;
    private UserDomainService service;
    private User user;
    private Cabinet cabinet;

    @BeforeEach
    void setUp() {
        UserRepositoryPort userRepository = mock(UserRepositoryPort.class);
        lentRepository = mock(LentRepositoryPort.class);
        service =
                new UserDomainService(
                        userRepository,
                        lentRepository,
                        mock(ItemHistoryRepositoryPort.class),
                        mock(AttendanceRepositoryPort.class),
                        mock(CoinHistoryRepositoryPort.class),
                        mock(ReservationPort.class));

        user = User.of("intra09", "intra09@example.com", null, UserRole.USER);
        ReflectionTestUtils.setField(user, "id", USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        cabinet = Cabinet.of(101, CabinetStatus.FULL, LentType.PRIVATE, 1, null, 1, "A", 1, 1);
        ReflectionTestUtils.setField(cabinet, "id", 101L);
    }

    private void activeLentExpiringAt(LocalDateTime expiredAt) {
        LentHistory lent = LentHistory.of(user, cabinet, expiredAt.minusDays(31), expiredAt);
        when(lentRepository.findByUserIdAndEndedAtIsNull(USER_ID)).thenReturn(Optional.of(lent));
    }

    @Test
    @DisplayName("대여 중이면 ISO 만료 시각과 남은 일수를 주고 기존 표시용 문자열도 그대로 둔다")
    void activeLentExposesExpiryFields() {
        LocalDateTime expiredAt = LocalDate.now().plusDays(7).atTime(14, 23, 11);
        activeLentExpiringAt(expiredAt);

        MyProfileResponseDto profile = service.getMyProfile(USER_ID);

        assertThat(profile.getExpiredAtIso()).isEqualTo(expiredAt);
        assertThat(profile.getDaysRemaining()).isEqualTo(7);
        assertThat(profile.getOverdue()).isFalse();
        // 호환: 기존 필드는 형식 그대로다.
        assertThat(profile.getExpiredAt())
                .isEqualTo(
                        expiredAt.format(
                                java.time.format.DateTimeFormatter.ofPattern("MM월 dd일 HH:mm")));
    }

    @Test
    @DisplayName("연체 중이면 남은 일수가 음수이고 overdue 가 true 다")
    void overdueLent() {
        activeLentExpiringAt(LocalDate.now().minusDays(3).atTime(9, 0));

        MyProfileResponseDto profile = service.getMyProfile(USER_ID);

        assertThat(profile.getDaysRemaining()).isEqualTo(-3);
        assertThat(profile.getOverdue()).isTrue();
    }

    @Test
    @DisplayName("대여 중이 아니면 세 필드는 모두 null 이다")
    void noActiveLent() {
        when(lentRepository.findByUserIdAndEndedAtIsNull(USER_ID)).thenReturn(Optional.empty());

        MyProfileResponseDto profile = service.getMyProfile(USER_ID);

        assertThat(profile.getExpiredAtIso()).isNull();
        assertThat(profile.getDaysRemaining()).isNull();
        assertThat(profile.getOverdue()).isNull();
        assertThat(profile.getExpiredAt()).isNull();
    }
}
