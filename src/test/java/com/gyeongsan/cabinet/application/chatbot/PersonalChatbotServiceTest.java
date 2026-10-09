package com.gyeongsan.cabinet.application.chatbot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gyeongsan.cabinet.domain.cabinet.model.Cabinet;
import com.gyeongsan.cabinet.domain.cabinet.model.CabinetStatus;
import com.gyeongsan.cabinet.domain.cabinet.model.LentType;
import com.gyeongsan.cabinet.domain.chatbot.model.PersonalAnswer;
import com.gyeongsan.cabinet.domain.chatbot.model.PersonalFacts;
import com.gyeongsan.cabinet.domain.chatbot.model.PersonalFacts.AllowedCabinets;
import com.gyeongsan.cabinet.domain.chatbot.model.PersonalFacts.Blocker;
import com.gyeongsan.cabinet.domain.chatbot.model.PersonalIntent;
import com.gyeongsan.cabinet.domain.chatbot.port.out.ChatbotMetricsPort;
import com.gyeongsan.cabinet.domain.item.model.ItemType;
import com.gyeongsan.cabinet.domain.item.port.out.ItemHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.model.LentHistory;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.domain.user.model.LentTicketRewardPolicy;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.model.UserRole;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PersonalChatbotServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    // 2026-10-07(수) 14:00
    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.of(2026, 10, 7, 14, 0).atZone(ZONE).toInstant(), ZONE);

    private final UserRepositoryPort users = mock(UserRepositoryPort.class);
    private final LentRepositoryPort lents = mock(LentRepositoryPort.class);
    private final ItemHistoryRepositoryPort items = mock(ItemHistoryRepositoryPort.class);
    private final LentTicketRewardPolicy policy = new LentTicketRewardPolicy(4800, 900);
    private final List<String> metricLog = new ArrayList<>();
    private final ChatbotMetricsPort metrics =
            new ChatbotMetricsPort() {
                @Override
                public void recordAsk(
                        com.gyeongsan.cabinet.domain.chatbot.model.ChatbotAnswer.Result result) {}

                @Override
                public void recordPersonal(PersonalIntent intent, String outcome) {
                    metricLog.add(intent + ":" + outcome);
                }
            };
    private PersonalChatbotService service;

    @BeforeEach
    void setUp() {
        service = newService(100);
    }

    private PersonalChatbotService newService(int perMinute) {
        return new PersonalChatbotService(
                users,
                lents,
                items,
                policy,
                new PerUserRateLimiter(perMinute, 60_000),
                metrics,
                CLOCK);
    }

    private static User.UserBuilder user(long id) {
        return User.builder()
                .id(id)
                .name("intra" + id)
                .email("secret" + id + "@example.com")
                .role(UserRole.USER);
    }

    private static LentHistory lent(User user, int visibleNum, LocalDateTime expiredAt) {
        Cabinet cabinet =
                Cabinet.of(visibleNum, CabinetStatus.FULL, LentType.PRIVATE, 1, null, 2, "A", 1, 1);
        return LentHistory.of(user, cabinet, LocalDateTime.of(2026, 9, 7, 10, 0), expiredAt);
    }

    private void stub(User user, LentHistory activeLent, int unusedTickets) {
        given(users.findById(user.getId())).willReturn(Optional.of(user));
        given(lents.findByUserIdAndEndedAtIsNull(user.getId()))
                .willReturn(Optional.ofNullable(activeLent));
        given(items.countByUserIdAndItemTypeAndUsedAtIsNull(user.getId(), ItemType.LENT))
                .willReturn(unusedTickets);
    }

    // ---- LENT_EXPIRY ----

    @Test
    @DisplayName("만료일: 달력 날짜 기준 남은 일수와 만료 시각을 알려 준다")
    void lentExpiry() {
        User u = user(7).build();
        stub(u, lent(u, 12, LocalDateTime.of(2026, 10, 10, 18, 0)), 0);

        PersonalAnswer answer = service.answer(7L, PersonalIntent.LENT_EXPIRY);

        PersonalFacts.LentExpiry facts = (PersonalFacts.LentExpiry) answer.facts();
        assertThat(facts.lentActive()).isTrue();
        assertThat(facts.visibleNum()).isEqualTo(12);
        assertThat(facts.daysRemaining()).isEqualTo(3);
        assertThat(facts.overdue()).isFalse();
        assertThat(answer.message())
                .contains("12번 사물함")
                .contains("2026년 10월 10일 18:00")
                .contains("3일 남았어요");
    }

    @Test
    @DisplayName("만료일 당일(시각 전)은 0일이고, 시각이 지나면 패널티 대상이라고만 알린다(만료일까지 안전하다고 약속하지 않는다)")
    void lentExpiryToday() {
        User u = user(7).build();
        stub(u, lent(u, 12, LocalDateTime.of(2026, 10, 7, 18, 0)), 0);

        PersonalAnswer answer = service.answer(7L, PersonalIntent.LENT_EXPIRY);

        PersonalFacts.LentExpiry facts = (PersonalFacts.LentExpiry) answer.facts();
        assertThat(facts.daysRemaining()).isZero();
        assertThat(facts.overdue()).isFalse();
        assertThat(answer.message()).contains("오늘이 만료일").contains("만료 시각이 지난 뒤에 반납하면 패널티 대상");
    }

    @Test
    @DisplayName("만료 시각이 지났으면 overdue 이고 남은 일수 문구를 쓰지 않는다")
    void lentExpiryOverdue() {
        User u = user(7).build();
        stub(u, lent(u, 12, LocalDateTime.of(2026, 10, 7, 9, 0)), 0);

        PersonalAnswer answer = service.answer(7L, PersonalIntent.LENT_EXPIRY);

        assertThat(((PersonalFacts.LentExpiry) answer.facts()).overdue()).isTrue();
        assertThat(answer.message()).contains("만료 시각이 지났어요").doesNotContain("남았어요");
    }

    @Test
    @DisplayName("만료일이 며칠 지난 경우도 남은 일수를 음수로 말하지 않는다")
    void lentExpiryLongOverdue() {
        User u = user(7).build();
        stub(u, lent(u, 12, LocalDateTime.of(2026, 10, 3, 9, 0)), 0);

        PersonalAnswer answer = service.answer(7L, PersonalIntent.LENT_EXPIRY);

        assertThat(answer.message()).contains("만료 시각이 지났어요").doesNotContain("-");
    }

    @Test
    @DisplayName("대여 중이 아니면 그렇게 알려 준다")
    void lentExpiryNone() {
        User u = user(7).build();
        stub(u, null, 0);

        PersonalAnswer answer = service.answer(7L, PersonalIntent.LENT_EXPIRY);

        assertThat(((PersonalFacts.LentExpiry) answer.facts()).lentActive()).isFalse();
        assertThat(answer.message()).contains("대여 중인 사물함이 없어요");
    }

    // ---- PENALTY_STATUS ----

    @Test
    @DisplayName("패널티: 남은 일수와 해제 예정일(오늘 + 일수)")
    void penalty() {
        User u = user(7).penaltyDays(3).build();
        stub(u, null, 0);

        PersonalAnswer answer = service.answer(7L, PersonalIntent.PENALTY_STATUS);

        PersonalFacts.PenaltyStatus facts = (PersonalFacts.PenaltyStatus) answer.facts();
        assertThat(facts.penaltyDays()).isEqualTo(3);
        assertThat(facts.releaseDate()).isEqualTo(LocalDate.of(2026, 10, 10));
        assertThat(answer.message()).contains("3일 남아").contains("2026년 10월 10일");
    }

    @Test
    @DisplayName("패널티가 없으면 해제일 없이 없다고 알린다")
    void penaltyNone() {
        User u = user(7).build();
        stub(u, null, 0);

        PersonalAnswer answer = service.answer(7L, PersonalIntent.PENALTY_STATUS);

        PersonalFacts.PenaltyStatus facts = (PersonalFacts.PenaltyStatus) answer.facts();
        assertThat(facts.penaltyDays()).isZero();
        assertThat(facts.releaseDate()).isNull();
        assertThat(answer.message()).contains("패널티가 없어요");
    }

    // ---- LENT_TICKET_CONDITION ----

    @Test
    @DisplayName("대여권 조건: 일반 사용자는 4800분 기준, 부족한 만큼과 다음 지급일(다음 달 1일)")
    void ticketConditionGeneral() {
        User u = user(7).monthlyLogtime(3000).build();
        stub(u, null, 0);

        PersonalAnswer answer = service.answer(7L, PersonalIntent.LENT_TICKET_CONDITION);

        PersonalFacts.TicketCondition facts = (PersonalFacts.TicketCondition) answer.facts();
        assertThat(facts.thresholdMinutes()).isEqualTo(4800);
        assertThat(facts.monthlyLogtimeMinutes()).isEqualTo(3000);
        assertThat(facts.meetsThreshold()).isFalse();
        assertThat(facts.hasUnusedTicket()).isFalse();
        assertThat(facts.nextPayDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(answer.message())
                .contains("80시간(4800분)")
                .contains("50시간(3000분)")
                .contains("30시간(1800분) 더 필요")
                .contains("2026년 11월 1일");
    }

    @Test
    @DisplayName("대여권 조건: 트센은 900분 기준이며, 이미 쓰지 않은 대여권이 있으면 지급이 생략된다고 알린다")
    void ticketConditionTranscender() {
        User u = user(7).monthlyLogtime(1000).ftGrade("Transcender").build();
        stub(u, null, 1);

        PersonalAnswer answer = service.answer(7L, PersonalIntent.LENT_TICKET_CONDITION);

        PersonalFacts.TicketCondition facts = (PersonalFacts.TicketCondition) answer.facts();
        assertThat(facts.thresholdMinutes()).isEqualTo(900);
        assertThat(facts.meetsThreshold()).isTrue();
        assertThat(facts.hasUnusedTicket()).isTrue();
        assertThat(answer.message()).contains("조건을 채웠어요").contains("새로 지급되지 않아요");
    }

    // ---- LENT_BLOCKER_DIAGNOSIS ----

    @Test
    @DisplayName("대여 불가 진단: 패널티·이미 대여 중·대여권 없음을 startLent 검사 순서대로 모두 알린다")
    void blockersAll() {
        User u = user(7).penaltyDays(2).build();
        stub(u, lent(u, 12, LocalDateTime.of(2026, 10, 20, 18, 0)), 0);

        PersonalAnswer answer = service.answer(7L, PersonalIntent.LENT_BLOCKER_DIAGNOSIS);

        PersonalFacts.LentBlockers facts = (PersonalFacts.LentBlockers) answer.facts();
        assertThat(facts.blockers())
                .containsExactly(Blocker.PENALTY, Blocker.ALREADY_LENDING, Blocker.NO_TICKET);
        assertThat(facts.activeVisibleNum()).isEqualTo(12);
        assertThat(facts.allowedCabinets()).isEqualTo(AllowedCabinets.EXCEPT_LAPISCINE);
        assertThat(answer.message())
                .contains("3가지")
                .contains("패널티가 2일")
                .contains("12번")
                .contains("대여권이 없어요");
    }

    @Test
    @DisplayName("막는 사유가 없으면 그렇게 말하고, 사물함 단위 사유(남의 예약 등)는 특정하지 않는다")
    void blockersNone() {
        User u = user(7).isPisciner(true).build();
        stub(u, null, 1);

        PersonalAnswer answer = service.answer(7L, PersonalIntent.LENT_BLOCKER_DIAGNOSIS);

        PersonalFacts.LentBlockers facts = (PersonalFacts.LentBlockers) answer.facts();
        assertThat(facts.blockers()).isEmpty();
        assertThat(facts.allowedCabinets()).isEqualTo(AllowedCabinets.LAPISCINE_ONLY);
        assertThat(answer.message())
                .contains("막는 사유가 없어요")
                .contains("다른 사람이 예약한 상태일 수 있어요")
                .contains("라피신 전용 사물함만");
    }

    // ---- 보안 경계 ----

    @Test
    @DisplayName("조회 대상은 인자로 받은 userId 하나뿐이다(다른 사용자는 조회하지 않고, 아무것도 저장하지 않는다)")
    void onlyOwnUserIsRead() {
        User me = user(7).monthlyLogtime(100).build();
        stub(me, null, 0);

        for (PersonalIntent intent : PersonalIntent.values()) {
            service.answer(7L, intent);
        }

        verify(users, org.mockito.Mockito.atLeastOnce()).findById(7L);
        verify(users, never()).findById(org.mockito.ArgumentMatchers.longThat(id -> id != 7L));
        verify(users, never()).save(any());
        verify(users, never()).findByName(any());
        verify(users, never()).findByEmail(any());
        verify(users, never()).findAll();
        verify(users, never()).findByIdWithLock(any());
        verify(lents, never()).save(any());
        verify(lents, never()).findAllActiveLents();
        verify(items, never()).save(any());
        verify(items, never()).saveAll(any());
        verify(items, never()).deleteAll(any());
    }

    @Test
    @DisplayName("응답 직렬화에 이름·이메일·코인 같은 값이 없다")
    void responseHasNoOtherUserFields() throws Exception {
        User me = user(7).penaltyDays(1).monthlyLogtime(100).coin(98765L).build();
        stub(me, lent(me, 12, LocalDateTime.of(2026, 10, 20, 18, 0)), 1);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

        for (PersonalIntent intent : PersonalIntent.values()) {
            String json = mapper.writeValueAsString(service.answer(7L, intent));
            assertThat(json)
                    .doesNotContain("intra7")
                    .doesNotContain("secret7@example.com")
                    .doesNotContain("98765")
                    .doesNotContain("coin")
                    .doesNotContain("email")
                    .doesNotContain("password")
                    .doesNotContain("returnMemo");
        }
    }

    @Test
    @DisplayName("없는 사용자, 삭제된 사용자는 같은 오류이고 상태를 구분해 알려 주지 않는다")
    void unknownOrDeletedUser() {
        given(users.findById(8L)).willReturn(Optional.empty());
        User deleted = user(9).deletedAt(LocalDateTime.of(2026, 9, 1, 0, 0)).build();
        given(users.findById(9L)).willReturn(Optional.of(deleted));

        for (long id : new long[] {8L, 9L}) {
            assertThatThrownBy(() -> service.answer(id, PersonalIntent.PENALTY_STATUS))
                    .isInstanceOfSatisfying(
                            ServiceException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
        }
        assertThatThrownBy(() -> service.answer(null, PersonalIntent.PENALTY_STATUS))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("사용자별 한도를 넘으면 DB 를 읽지 않고 429 로 거절하며, 다른 사용자에는 영향이 없다")
    void rateLimited() {
        PersonalChatbotService limited = newService(2);
        User u = user(7).build();
        stub(u, null, 0);
        User other = user(8).build();
        stub(other, null, 0);

        limited.answer(7L, PersonalIntent.PENALTY_STATUS);
        limited.answer(7L, PersonalIntent.PENALTY_STATUS);
        org.mockito.Mockito.clearInvocations(users, lents, items);

        assertThatThrownBy(() -> limited.answer(7L, PersonalIntent.PENALTY_STATUS))
                .isInstanceOfSatisfying(
                        ServiceException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TOO_MANY_REQUESTS));
        verifyNoInteractions(users, lents, items);
        assertThat(metricLog).contains("PENALTY_STATUS:rate_limited");

        assertThat(limited.answer(8L, PersonalIntent.PENALTY_STATUS)).isNotNull();
        verifyNoMoreInteractions(lents, items);
    }

    @Test
    @DisplayName("지표는 인텐트와 결과 종류만 남기고 사용자 정보는 담지 않는다")
    void metricsCarryNoUserData() {
        User u = user(7).build();
        stub(u, null, 0);

        service.answer(7L, PersonalIntent.LENT_EXPIRY);

        assertThat(metricLog).containsExactly("LENT_EXPIRY:answered");
    }
}
