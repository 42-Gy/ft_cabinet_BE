package com.gyeongsan.cabinet.application.lent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gyeongsan.cabinet.domain.cabinet.model.Cabinet;
import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import com.gyeongsan.cabinet.domain.cabinet.model.LentType;
import com.gyeongsan.cabinet.domain.cabinet.port.out.CabinetRepositoryPort;
import com.gyeongsan.cabinet.domain.item.port.out.ItemHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.model.LentHistory;
import com.gyeongsan.cabinet.domain.lent.model.LentReturnResult;
import com.gyeongsan.cabinet.domain.lent.port.out.AiCheckPort;
import com.gyeongsan.cabinet.domain.lent.port.out.ImageUploadPort;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.ReservationPort;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

/** 반납 결과(잔여일/연체/패널티)가 실제 패널티 로직과 같은 기준으로 만들어지는지 확인한다. */
class LentReturnResultTest {

    private static final Long USER_ID = 9L;

    private LentRepositoryPort lentRepository;
    private LentApplicationService service;
    private User user;
    private Cabinet cabinet;

    @BeforeEach
    void setUp() {
        lentRepository = mock(LentRepositoryPort.class);
        service =
                new LentApplicationService(
                        mock(UserRepositoryPort.class),
                        mock(CabinetRepositoryPort.class),
                        lentRepository,
                        mock(ItemHistoryRepositoryPort.class),
                        mock(ReservationPort.class),
                        mock(AiCheckPort.class),
                        mock(ImageUploadPort.class),
                        mock(TransactionTemplate.class));

        user = User.of("intra09", "intra09@example.com", null, UserRole.USER);
        ReflectionTestUtils.setField(user, "id", USER_ID);
        cabinet = Cabinet.of(101, CabinetStatus.FULL, LentType.PRIVATE, 1, null, 1, "A", 1, 1);
    }

    private LentHistory activeLentExpiringAt(LocalDateTime expiredAt) {
        LentHistory lent = LentHistory.of(user, cabinet, expiredAt.minusDays(31), expiredAt);
        when(lentRepository.findByUserIdAndEndedAtIsNull(USER_ID)).thenReturn(Optional.of(lent));
        return lent;
    }

    @Test
    @DisplayName("기간이 남은 채로 반납하면 남은 일수가 양수이고 패널티는 없다")
    void returnBeforeExpiry() {
        LocalDateTime expiredAt = LocalDate.now().plusDays(5).atTime(23, 59);
        LentHistory lent = activeLentExpiringAt(expiredAt);

        LentReturnResult result = service.processReturnTransaction(USER_ID, "1234", "photo");

        assertThat(result.expiredAt()).isEqualTo(expiredAt);
        assertThat(result.daysRemaining()).isEqualTo(5);
        assertThat(result.overdue()).isFalse();
        assertThat(result.penaltyAppliedDays()).isZero();
        assertThat(result.returnedAt()).isEqualTo(lent.getEndedAt());
        assertThat(user.getPenaltyDays()).isZero();
        assertThat(cabinet.getStatus()).isEqualTo(CabinetStatus.AVAILABLE);
    }

    @Test
    @DisplayName("만료일 당일, 만료 시각 전에 반납하면 0일 남음이고 연체가 아니다")
    void returnOnExpiryDayBeforeExpiry() {
        activeLentExpiringAt(LocalDate.now().atTime(23, 59, 59));

        LentReturnResult result = service.processReturnTransaction(USER_ID, "1234", "photo");

        assertThat(result.daysRemaining()).isZero();
        assertThat(result.overdue()).isFalse();
        assertThat(result.penaltyAppliedDays()).isZero();
    }

    @Test
    @DisplayName("만료일 당일이라도 만료 시각이 지났으면 연체이고 패널티 3일이 붙는다 (현재 패널티 로직 그대로)")
    void returnOnExpiryDayAfterExpiry() {
        // 표시는 현재 패널티 판정과 같아야 한다. 화면의 경고와 실제 패널티가 어긋나면 안 된다.
        activeLentExpiringAt(LocalDate.now().atStartOfDay());

        LentReturnResult result = service.processReturnTransaction(USER_ID, "1234", "photo");

        assertThat(result.daysRemaining()).isZero();
        assertThat(result.overdue()).isTrue();
        assertThat(result.penaltyAppliedDays()).isEqualTo(3);
        assertThat(user.getPenaltyDays()).isEqualTo(3);
    }

    @Test
    @DisplayName("며칠 연체된 채 반납하면 남은 일수가 음수이고 연체일 x 3 만큼 패널티가 붙는다")
    void returnAfterOverdueDays() {
        activeLentExpiringAt(LocalDate.now().minusDays(2).atStartOfDay());

        LentReturnResult result = service.processReturnTransaction(USER_ID, "1234", "photo");

        assertThat(result.daysRemaining()).isEqualTo(-2);
        assertThat(result.overdue()).isTrue();
        assertThat(result.penaltyAppliedDays()).isEqualTo(6);
        assertThat(user.getPenaltyDays()).isEqualTo(6);
    }

    @Test
    @DisplayName("기존 패널티가 있어도 이번에 새로 붙은 일수만 알려 준다")
    void penaltyAppliedIsOnlyTheNewOne() {
        user.applyPenalty(10);
        activeLentExpiringAt(LocalDate.now().minusDays(1).atStartOfDay());

        LentReturnResult result = service.processReturnTransaction(USER_ID, "1234", "photo");

        assertThat(result.penaltyAppliedDays()).isEqualTo(3);
        assertThat(user.getPenaltyDays()).isEqualTo(13);
    }

    @Test
    @DisplayName("수동(강제) 반납도 같은 결과를 돌려주고 사물함은 PENDING 이 된다")
    void manualReturnReturnsSameKindOfResult() {
        activeLentExpiringAt(LocalDate.now().minusDays(1).atStartOfDay());

        LentReturnResult result =
                service.endLentManual(USER_ID, "1234", "[User Force] 사유", "photo");

        assertThat(result.daysRemaining()).isEqualTo(-1);
        assertThat(result.overdue()).isTrue();
        assertThat(result.penaltyAppliedDays()).isEqualTo(3);
        assertThat(cabinet.getStatus()).isEqualTo(CabinetStatus.PENDING);
    }
}
