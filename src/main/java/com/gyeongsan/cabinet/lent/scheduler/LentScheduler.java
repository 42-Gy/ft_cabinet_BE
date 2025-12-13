package com.gyeongsan.cabinet.lent.scheduler;

import com.gyeongsan.cabinet.alarm.dto.AlarmEvent;
import com.gyeongsan.cabinet.cabinet.domain.Cabinet;
import com.gyeongsan.cabinet.cabinet.domain.CabinetStatus;
import com.gyeongsan.cabinet.lent.domain.LentHistory;
import com.gyeongsan.cabinet.lent.repository.LentRepository;
import com.gyeongsan.cabinet.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LentScheduler {

    private final LentRepository lentRepository;
    private final ApplicationEventPublisher eventPublisher;

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
            if (overdueDays <= 0) overdueDays = 1;

            int newPenalty = (int) (overdueDays * overdueDays);
            user.updatePenaltyDays(newPenalty);

            if (cabinet.getStatus() != CabinetStatus.OVERDUE) {
                cabinet.updateStatus(CabinetStatus.OVERDUE);
                sendOverdueAlarm(user, cabinet.getId());
            }

            log.info("🚨 연체 처리: 유저={}, 연체일={}일, 패널티={}일",
                    user.getName(), overdueDays, newPenalty);
        }
    }

    private void sendOverdueAlarm(User user, Long cabinetId) {
        String message = String.format(
                "🚨 *[연체 경고]*\n%s님, %d번 사물함이 연체되었습니다. 패널티가 누적되고 있으니 즉시 반납해주세요!",
                user.getName(), cabinetId
        );
        eventPublisher.publishEvent(new AlarmEvent(user.getEmail(), message));
    }
}
