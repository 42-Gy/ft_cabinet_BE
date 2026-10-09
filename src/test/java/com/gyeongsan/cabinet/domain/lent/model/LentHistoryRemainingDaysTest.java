package com.gyeongsan.cabinet.domain.lent.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.gyeongsan.cabinet.domain.cabinet.model.Cabinet;
import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import com.gyeongsan.cabinet.domain.cabinet.model.LentType;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LentHistoryRemainingDaysTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);

    private LentHistory lentExpiringAt(LocalDateTime expiredAt) {
        User user = User.of("intra09", "intra09@example.com", null, UserRole.USER);
        Cabinet cabinet =
                Cabinet.of(101, CabinetStatus.FULL, LentType.PRIVATE, 1, null, 1, "A", 1, 1);
        return LentHistory.of(user, cabinet, expiredAt.minusDays(31), expiredAt);
    }

    @Test
    @DisplayName("시각과 무관하게 만료일의 날짜로만 남은 일수를 센다")
    void countsCalendarDaysIgnoringTimeOfDay() {
        // 만료 시각이 자정 직전이어도, 직후여도 같은 날짜면 같은 일수다.
        assertThat(lentExpiringAt(TODAY.plusDays(3).atStartOfDay()).calculateRemainingDays(TODAY))
                .isEqualTo(3);
        assertThat(
                        lentExpiringAt(TODAY.plusDays(3).atTime(23, 59, 59))
                                .calculateRemainingDays(TODAY))
                .isEqualTo(3);
    }

    @Test
    @DisplayName("만료일 당일은 0, 하루 전은 1 이다")
    void expiryDayIsZero() {
        assertThat(lentExpiringAt(TODAY.atTime(14, 23)).calculateRemainingDays(TODAY)).isZero();
        assertThat(lentExpiringAt(TODAY.plusDays(1).atTime(0, 1)).calculateRemainingDays(TODAY))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("만료일이 지났으면 지난 일수만큼 음수다")
    void negativeWhenPastExpiryDate() {
        assertThat(lentExpiringAt(TODAY.minusDays(1).atTime(23, 59)).calculateRemainingDays(TODAY))
                .isEqualTo(-1);
        assertThat(lentExpiringAt(TODAY.minusDays(5).atTime(0, 0)).calculateRemainingDays(TODAY))
                .isEqualTo(-5);
    }

    @Test
    @DisplayName("월·연 경계를 넘어도 달력 기준으로 센다")
    void crossesMonthAndYearBoundary() {
        LocalDate dec30 = LocalDate.of(2026, 12, 30);
        assertThat(
                        lentExpiringAt(LocalDate.of(2027, 1, 2).atTime(9, 0))
                                .calculateRemainingDays(dec30))
                .isEqualTo(3);
    }
}
