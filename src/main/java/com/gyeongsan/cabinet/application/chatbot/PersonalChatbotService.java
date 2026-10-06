package com.gyeongsan.cabinet.application.chatbot;

import com.gyeongsan.cabinet.domain.chatbot.model.PersonalAnswer;
import com.gyeongsan.cabinet.domain.chatbot.model.PersonalFacts;
import com.gyeongsan.cabinet.domain.chatbot.model.PersonalFacts.AllowedCabinets;
import com.gyeongsan.cabinet.domain.chatbot.model.PersonalFacts.Blocker;
import com.gyeongsan.cabinet.domain.chatbot.model.PersonalIntent;
import com.gyeongsan.cabinet.domain.chatbot.port.in.PersonalChatbotUseCase;
import com.gyeongsan.cabinet.domain.chatbot.port.out.ChatbotMetricsPort;
import com.gyeongsan.cabinet.domain.item.model.ItemType;
import com.gyeongsan.cabinet.domain.item.port.out.ItemHistoryRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.model.LentHistory;
import com.gyeongsan.cabinet.domain.lent.port.out.LentRepositoryPort;
import com.gyeongsan.cabinet.domain.user.model.LentTicketRewardPolicy;
import com.gyeongsan.cabinet.domain.user.model.User;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인한 본인의 정보로 답하는 개인화 챗봇. 지켜야 할 규칙:
 *
 * <ul>
 *   <li>대상은 인자로 받은 userId(인증 정보에서 얻은 값) 하나뿐이다. 질문 문장이나 요청 값으로 대상을 바꿀 수 없다.
 *   <li>읽기 전용이다(락도, 상태 변경도 없다). 대여 흐름과 락 경합하지 않는다.
 *   <li>인텐트마다 필요한 값만 읽고 {@link PersonalFacts} 의 화이트리스트로만 내보낸다.
 *   <li>값은 로그에 남기지 않는다(예외 메시지에도 넣지 않는다).
 * </ul>
 */
@Transactional(readOnly = true)
public class PersonalChatbotService implements PersonalChatbotUseCase {

    private final UserRepositoryPort userRepository;
    private final LentRepositoryPort lentRepository;
    private final ItemHistoryRepositoryPort itemHistoryRepository;
    private final LentTicketRewardPolicy rewardPolicy;
    private final PerUserRateLimiter rateLimiter;
    private final ChatbotMetricsPort metrics;
    private final Clock clock;

    public PersonalChatbotService(
            UserRepositoryPort userRepository,
            LentRepositoryPort lentRepository,
            ItemHistoryRepositoryPort itemHistoryRepository,
            LentTicketRewardPolicy rewardPolicy,
            PerUserRateLimiter rateLimiter,
            ChatbotMetricsPort metrics,
            Clock clock) {
        this.userRepository = userRepository;
        this.lentRepository = lentRepository;
        this.itemHistoryRepository = itemHistoryRepository;
        this.rewardPolicy = rewardPolicy;
        this.rateLimiter = rateLimiter;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Override
    public PersonalAnswer answer(Long userId, PersonalIntent intent) {
        if (userId == null || intent == null) {
            throw new ServiceException(ErrorCode.USER_NOT_FOUND);
        }
        if (!rateLimiter.tryAcquire(userId)) {
            metrics.recordPersonal(intent, "rate_limited");
            throw new ServiceException(ErrorCode.TOO_MANY_REQUESTS);
        }
        User user =
                userRepository
                        .findById(userId)
                        .filter(u -> u.getDeletedAt() == null)
                        .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now(clock);
        PersonalAnswer answer =
                switch (intent) {
                    case LENT_EXPIRY -> lentExpiry(user, now);
                    case PENALTY_STATUS -> penalty(user, now.toLocalDate());
                    case LENT_TICKET_CONDITION -> ticketCondition(user, now.toLocalDate());
                    case LENT_BLOCKER_DIAGNOSIS -> blockers(user, now.toLocalDate());
                };
        metrics.recordPersonal(intent, "answered");
        return answer;
    }

    private PersonalAnswer lentExpiry(User user, LocalDateTime now) {
        PersonalFacts.LentExpiry facts =
                lentRepository
                        .findByUserIdAndEndedAtIsNull(user.getId())
                        .map(lent -> expiryOf(lent, now))
                        .orElse(new PersonalFacts.LentExpiry(false, null, null, null, null));
        return new PersonalAnswer(
                PersonalIntent.LENT_EXPIRY, PersonalAnswerTemplates.lentExpiry(facts), facts);
    }

    /** `/me` 와 같은 계산(달력 날짜 기준 남은 일수, 만료 시각 기준 overdue)을 쓴다. */
    private static PersonalFacts.LentExpiry expiryOf(LentHistory lent, LocalDateTime now) {
        return new PersonalFacts.LentExpiry(
                true,
                lent.getCabinet().getVisibleNum(),
                lent.getExpiredAt(),
                lent.calculateRemainingDays(now.toLocalDate()),
                lent.isOverdue(now));
    }

    private PersonalAnswer penalty(User user, LocalDate today) {
        int days = penaltyDaysOf(user);
        PersonalFacts.PenaltyStatus facts =
                new PersonalFacts.PenaltyStatus(days, days > 0 ? today.plusDays(days) : null);
        return new PersonalAnswer(
                PersonalIntent.PENALTY_STATUS, PersonalAnswerTemplates.penalty(facts), facts);
    }

    private PersonalAnswer ticketCondition(User user, LocalDate today) {
        int threshold = rewardPolicy.thresholdMinutesFor(user);
        int logtime = user.getMonthlyLogtime() == null ? 0 : user.getMonthlyLogtime();
        PersonalFacts.TicketCondition facts =
                new PersonalFacts.TicketCondition(
                        threshold,
                        logtime,
                        rewardPolicy.qualifies(user, logtime),
                        hasUnusedTicket(user.getId()),
                        today.withDayOfMonth(1).plusMonths(1));
        return new PersonalAnswer(
                PersonalIntent.LENT_TICKET_CONDITION,
                PersonalAnswerTemplates.ticketCondition(facts),
                facts);
    }

    /**
     * 사용자 조건으로 대여가 막히는 사유. {@code startLent} 의 사용자 단위 검사(패널티, 이미 대여 중, 대여권)만 본다. 사물함 단위 사유(남의 예약,
     * 사물함 상태)는 특정 사물함을 지정해야 알 수 있고 다른 사람의 정보가 새어 나갈 수 있어 판단하지 않는다.
     */
    private PersonalAnswer blockers(User user, LocalDate today) {
        List<Blocker> blockers = new ArrayList<>();
        int penaltyDays = penaltyDaysOf(user);
        if (penaltyDays > 0) {
            blockers.add(Blocker.PENALTY);
        }
        Integer activeVisibleNum =
                lentRepository
                        .findByUserIdAndEndedAtIsNull(user.getId())
                        .map(lent -> lent.getCabinet().getVisibleNum())
                        .orElse(null);
        if (activeVisibleNum != null) {
            blockers.add(Blocker.ALREADY_LENDING);
        }
        if (!hasUnusedTicket(user.getId())) {
            blockers.add(Blocker.NO_TICKET);
        }
        PersonalFacts.LentBlockers facts =
                new PersonalFacts.LentBlockers(
                        List.copyOf(blockers),
                        penaltyDays,
                        penaltyDays > 0 ? today.plusDays(penaltyDays) : null,
                        activeVisibleNum,
                        user.isPisciner()
                                ? AllowedCabinets.LAPISCINE_ONLY
                                : AllowedCabinets.EXCEPT_LAPISCINE);
        return new PersonalAnswer(
                PersonalIntent.LENT_BLOCKER_DIAGNOSIS,
                PersonalAnswerTemplates.blockers(facts),
                facts);
    }

    private boolean hasUnusedTicket(Long userId) {
        return itemHistoryRepository.countByUserIdAndItemTypeAndUsedAtIsNull(userId, ItemType.LENT)
                > 0;
    }

    private static int penaltyDaysOf(User user) {
        Integer days = user.getPenaltyDays();
        return days == null ? 0 : Math.max(days, 0);
    }
}
