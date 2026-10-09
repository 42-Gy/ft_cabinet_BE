package com.gyeongsan.cabinet.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gyeongsan.cabinet.domain.coin.port.out.CoinHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.item.model.Item;
import com.gyeongsan.cabinet.domain.item.model.ItemHistory;
import com.gyeongsan.cabinet.domain.item.model.ItemType;
import com.gyeongsan.cabinet.domain.item.port.out.ItemHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.ReservationPort;
import com.gyeongsan.cabinet.domain.user.model.FtGradeSnapshot;
import com.gyeongsan.cabinet.domain.user.model.LentTicketRewardPolicy;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.domain.user.port.out.AttendanceRepositoryPort;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** 월간 로그타임 보상: 트센 15시간 / 일반 80시간 기준, 같은 달 중복 지급 방지. */
class UserDomainServiceRewardTest {

    private static final Long USER_ID = 9L;

    private UserRepositoryPort userRepository;
    private ItemHistoryRepositoryPort itemHistoryRepository;
    private UserDomainService service;
    private Item lentTicket;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepositoryPort.class);
        itemHistoryRepository = mock(ItemHistoryRepositoryPort.class);
        service =
                new UserDomainService(
                        userRepository,
                        mock(LentRepositoryPort.class),
                        itemHistoryRepository,
                        mock(AttendanceRepositoryPort.class),
                        mock(CoinHistoryRepositoryPort.class),
                        mock(ReservationPort.class),
                        new LentTicketRewardPolicy(4800, 900));
        lentTicket = new Item("대여권", ItemType.LENT, 0L, "대여권");
        when(itemHistoryRepository.countByUserIdAndItemTypeAndUsedAtIsNull(USER_ID, ItemType.LENT))
                .thenReturn(0);
    }

    private User user(String grade) {
        return user(grade, false);
    }

    private User user(String grade, boolean pisciner) {
        User user = User.of("test-user", "test@example.com", UserRole.USER, pisciner);
        ReflectionTestUtils.setField(user, "id", USER_ID);
        user.updateFtGrade(grade);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    @DisplayName("일반 사용자: 4799분이면 지급되지 않고, 4800분이면 지급된다 (둘 다 로그타임은 초기화)")
    void defaultUserBoundary() {
        User below = user("Cadet");
        service.processLogtimeTransaction(USER_ID, lentTicket, 4799, true);
        verify(itemHistoryRepository, never()).save(any(ItemHistory.class));
        assertThat(below.getMonthlyLogtime()).isZero();

        User atThreshold = user("Cadet");
        service.processLogtimeTransaction(USER_ID, lentTicket, 4800, true);
        verify(itemHistoryRepository, times(1)).save(any(ItemHistory.class));
        assertThat(atThreshold.getMonthlyLogtime()).isZero();
    }

    @Test
    @DisplayName("트센: 899분이면 지급되지 않고, 900분이면 지급된다")
    void transcenderBoundary() {
        user("Transcender");
        service.processLogtimeTransaction(USER_ID, lentTicket, 899, true);
        verify(itemHistoryRepository, never()).save(any(ItemHistory.class));

        service.processLogtimeTransaction(USER_ID, lentTicket, 900, true);
        verify(itemHistoryRepository, times(1)).save(any(ItemHistory.class));
    }

    @Test
    @DisplayName("트센이 두 기준을 모두 만족해도(80시간 이상) 대여권은 정확히 1개만 지급된다")
    void transcenderMeetingBothThresholdsGetsExactlyOne() {
        user("Transcender");

        service.processLogtimeTransaction(USER_ID, lentTicket, 6000, true);

        verify(itemHistoryRepository, times(1)).save(any(ItemHistory.class));
    }

    @Test
    @DisplayName("트센이 아닌 사용자는 15시간 이상이어도 지급되지 않는다 (미확인 grade 포함)")
    void nonTranscenderGetsNothingAtFifteenHours() {
        user("Cadet");
        service.processLogtimeTransaction(USER_ID, lentTicket, 1200, true);

        user("Member");
        service.processLogtimeTransaction(USER_ID, lentTicket, 1200, true);

        user(null);
        service.processLogtimeTransaction(USER_ID, lentTicket, 1200, true);

        verify(itemHistoryRepository, never()).save(any(ItemHistory.class));
    }

    @Test
    @DisplayName("이미 미사용 대여권이 있으면 지급을 생략하지만, 월간 로그타임은 그래도 초기화된다 (기존 동작)")
    void skippedWhenHoldingTicketStillResetsLogtime() {
        User transcender = user("Transcender");
        when(itemHistoryRepository.countByUserIdAndItemTypeAndUsedAtIsNull(USER_ID, ItemType.LENT))
                .thenReturn(1);

        service.processLogtimeTransaction(USER_ID, lentTicket, 5000, true);

        verify(itemHistoryRepository, never()).save(any(ItemHistory.class));
        assertThat(transcender.getMonthlyLogtime()).isZero();
    }

    @Test
    @DisplayName("지급일이 아니면 로그타임만 갱신하고 지급도 초기화도 하지 않는다")
    void notPayDay() {
        User transcender = user("Transcender");

        service.processLogtimeTransaction(USER_ID, lentTicket, 5000, false);

        verify(itemHistoryRepository, never()).save(any(ItemHistory.class));
        assertThat(transcender.getMonthlyLogtime()).isEqualTo(5000);
    }

    @Test
    @DisplayName("지급할 대여권 아이템이 없으면 지급하지 않는다")
    void noTicketItem() {
        user("Transcender");

        service.processLogtimeTransaction(USER_ID, null, 5000, true);

        verify(itemHistoryRepository, never()).save(any(ItemHistory.class));
    }

    @Test
    @DisplayName("지급일 재조회 대상 판정: 15시간 이상 80시간 미만의 비-트센 비-피시너만")
    void needsGradeRefresh() {
        user("Cadet");
        assertThat(service.needsGradeRefresh(USER_ID, 899)).isFalse();
        assertThat(service.needsGradeRefresh(USER_ID, 900)).isTrue();
        assertThat(service.needsGradeRefresh(USER_ID, 4800)).isFalse();

        user("Transcender");
        assertThat(service.needsGradeRefresh(USER_ID, 1000)).isFalse();

        user(null, true);
        assertThat(service.needsGradeRefresh(USER_ID, 1000)).isFalse();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        assertThat(service.needsGradeRefresh(USER_ID, 1000)).isFalse();
    }

    @Test
    @DisplayName("grade 저장: 정상 값은 저장하고, 해석 실패/형식 오류는 기존 값을 유지한다")
    void updateFtGrade() {
        User user = user("Cadet");

        service.updateFtGrade(USER_ID, FtGradeSnapshot.unparsed());
        assertThat(user.getFtGrade()).isEqualTo("Cadet");

        service.updateFtGrade(USER_ID, FtGradeSnapshot.ofRejected());
        assertThat(user.getFtGrade()).isEqualTo("Cadet");

        service.updateFtGrade(USER_ID, FtGradeSnapshot.of("Transcender"));
        assertThat(user.getFtGrade()).isEqualTo("Transcender");
        assertThat(user.isTranscender()).isTrue();

        // 본과정 항목이 없어진 경우(null)는 정상 해석 결과이므로 null 로 갱신한다.
        service.updateFtGrade(USER_ID, FtGradeSnapshot.of(null));
        assertThat(user.getFtGrade()).isNull();
    }

    @Test
    @DisplayName("재조회로 트센이 확인되면 같은 지급일에 15시간 기준으로 지급된다")
    void refreshedTranscenderIsRewardedWithFifteenHourRule() {
        User user = user("Cadet");
        service.processLogtimeTransaction(USER_ID, lentTicket, 1000, false);
        verify(itemHistoryRepository, never()).save(any(ItemHistory.class));

        service.updateFtGrade(USER_ID, FtGradeSnapshot.of("Transcender"));
        service.processLogtimeTransaction(USER_ID, lentTicket, 1000, true);

        verify(itemHistoryRepository, times(1)).save(any(ItemHistory.class));
        assertThat(user.getMonthlyLogtime()).isZero();
    }
}
