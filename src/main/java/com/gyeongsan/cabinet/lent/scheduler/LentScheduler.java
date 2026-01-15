package com.gyeongsan.cabinet.lent.scheduler;

import com.gyeongsan.cabinet.alarm.dto.AlarmEvent;
import com.gyeongsan.cabinet.cabinet.domain.Cabinet;
import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;
import com.gyeongsan.cabinet.lent.domain.LentHistory;
import com.gyeongsan.cabinet.lent.repository.LentRepository;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.item.domain.ItemHistory;
import com.gyeongsan.cabinet.item.domain.ItemType;
import com.gyeongsan.cabinet.item.repository.ItemHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LentScheduler {

    private final LentRepository lentRepository;
    private final ItemHistoryRepository itemHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${cabinet.policy.lent-term}")
    private Integer lentTerm;

    @Scheduled(cron = "0 0 0 1 * *")
    @Transactional
    public void monthlyProcess() {
        log.info("📅 [Monthly] 월간 정기 작업 시작");

        grantRentalTicket();
        // autoExtension(); -> Moved to Daily Schedule
        handleExpiration();

        log.info("✅ [Monthly] 월간 정기 작업 완료");
    }

    private void grantRentalTicket() {

        log.info("1. [Grant] 대여권 지급 시작...");
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void autoExtension() {
        log.info("🔔 [Daily] 자동 연장 프로세스 시작...");
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime startOfDay = tomorrow.atStartOfDay();
        LocalDateTime endOfDay = tomorrow.atTime(LocalTime.MAX);

        List<LentHistory> expiringLents = lentRepository.findAllActiveLentsByExpiredAtBetween(startOfDay, endOfDay);

        int extendedCount = 0;

        for (LentHistory lent : expiringLents) {
            if (!lent.isAutoExtension()) {
                continue;
            }
            User user = lent.getUser();
            List<ItemHistory> tickets = itemHistoryRepository.findUnusedItems(user.getId(), ItemType.LENT);

            if (!tickets.isEmpty()) {
                ItemHistory ticket = tickets.get(0);
                ticket.use();
                lent.extendExpiration(lentTerm.longValue());
                extendedCount++;
                log.info("✅ 자동 연장 성공: User={}, NewExpiredAt={}", user.getName(), lent.getExpiredAt());
            }
        }

        log.info("✅ 총 {}명의 대여가 자동 연장되었습니다.", extendedCount);
    }

    private void handleExpiration() {

        log.info("3. [Expiration] 만료 및 연체 처리 시작...");
        checkOverdue();
    }

    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void checkOverdue() {
        LocalDateTime now = LocalDateTime.now();
        log.info("⏰ 연체자 단속 시작! (현재 시각: {})", now);

        List<LentHistory> overdueLents = lentRepository.findAllOverdueLentHistories(now);

        if (overdueLents.isEmpty()) {
            log.info(" - 다행히 연체자가 없습니다.");
            return;
        }

        for (LentHistory lh : overdueLents) {
            User user = lh.getUser();
            Cabinet cabinet = lh.getCabinet();

            long overdueDays = ChronoUnit.DAYS.between(lh.getExpiredAt(), now);
            if (overdueDays <= 0)
                overdueDays = 1;

            int newPenalty = (int) (overdueDays * 3); // Squared -> Multiply by 3
            user.updatePenaltyDays(newPenalty);

            if (cabinet.getStatus() != CabinetStatus.OVERDUE) {
                cabinet.updateStatus(CabinetStatus.OVERDUE);
                sendOverdueAlarm(user, cabinet.getId());
            }

            log.info(
                    "🚨 연체 처리: 유저={}, 연체일={}일, 패널티={}일",
                    user.getName(), overdueDays, newPenalty);
        }
    }

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional(readOnly = true)
    public void checkExpirationImminent() {
        log.info("🔔 [D-7, D-1] 반납 임박 알림 체크 시작");
        LocalDate today = LocalDate.now();

        // Check D-7
        checkAndSendAlarm(today.plusDays(7), 7);

        // Check D-1
        checkAndSendAlarm(today.plusDays(1), 1);

        log.info("✅ 반납 임박 알림 전송 로직 완료");
    }

    private void checkAndSendAlarm(LocalDate targetDate, int daysLeft) {
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.atTime(LocalTime.MAX);

        List<LentHistory> targetLents = lentRepository.findAllActiveLentsByExpiredAtBetween(startOfDay, endOfDay);

        if (targetLents.isEmpty()) {
            log.info(" - {}일 뒤 반납 예정자가 없습니다.", daysLeft);
            return;
        }

        for (LentHistory lh : targetLents) {
            sendImminentAlarm(
                    lh.getUser(),
                    lh.getExpiredAt(),
                    lh.getCabinet().getVisibleNum(),
                    daysLeft);
        }
        log.info(" - {}일 전 알림: {}명 전송 완료", daysLeft, targetLents.size());

    }

    private void sendOverdueAlarm(User user, Long cabinetId) {
        String message = String.format(
                "🚨 *[연체 경고]*\n%s님, %d번 사물함이 연체되었습니다. 패널티가 누적되고 있으니 즉시 반납해주세요!",
                user.getName(), cabinetId);
        eventPublisher.publishEvent(new AlarmEvent(user.getEmail(), message));
    }

    private void sendImminentAlarm(User user, LocalDateTime expiredAt, Integer visibleNum, int daysLeft) {
        String dateStr = expiredAt.toLocalDate().toString();
        String message = String.format(
                "⏳ *[반납 알림]*\n%s님, 사용 중인 사물함(%d번)의 반납 기한이 %d일 남았습니다.\n(반납 예정일: %s)\n잊지 말고 반납해주세요! 😊",
                user.getName(), visibleNum, daysLeft, dateStr);
        eventPublisher.publishEvent(new AlarmEvent(user.getEmail(), message));
    }
}
