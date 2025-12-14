package com.gyeongsan.cabinet.user.scheduler;

import com.gyeongsan.cabinet.item.domain.Item;
import com.gyeongsan.cabinet.item.domain.ItemHistory;
import com.gyeongsan.cabinet.item.domain.ItemType;
import com.gyeongsan.cabinet.item.repository.ItemHistoryRepository;
import com.gyeongsan.cabinet.item.repository.ItemRepository;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import com.gyeongsan.cabinet.utils.FtApiManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Log4j2
public class LogtimeScheduler {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ItemHistoryRepository itemHistoryRepository;
    private final FtApiManager ftApiManager;

    private static final int MONTHLY_TARGET_MINUTES = 3000;

    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void processDailyLogtime() {
        log.info("📅 [Daily] 로그타임 집계 시작");

        boolean isPayDay = LocalDate.now().getDayOfMonth() == 1;

        Item lentTicketItem = null;
        if (isPayDay) {
            lentTicketItem = itemRepository.findAll().stream()
                    .filter(i -> i.getType() == ItemType.LENT)
                    .findFirst()
                    .orElse(null);

            if (lentTicketItem == null) {
                log.error("⚠️ [Error] 보상 지급 실패: LENT 아이템이 DB에 없습니다.");
            }
        }

        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            try {
                int minutes = ftApiManager.getYesterdayLogtimeMinutes(user.getName());

                if (minutes > 0) {
                    user.addMonthlyLogtime(minutes);
                    log.info("User {}: +{}분 (누적: {}분)", user.getName(), minutes, user.getMonthlyLogtime());
                }

                if (isPayDay) {
                    if (lentTicketItem != null && user.getMonthlyLogtime() >= MONTHLY_TARGET_MINUTES) {
                        giveLentTicket(user, lentTicketItem);
                    }
                    user.resetMonthlyLogtime();
                }

                Thread.sleep(100);

            } catch (Exception e) {
                log.error("{} 로그타임 처리 중 에러: {}", user.getName(), e.getMessage());
            }
        }

        if (isPayDay) {
            log.info("✅ [Monthly] 월간 보상 지급 및 초기화 완료");
        } else {
            log.info("✅ [Daily] 일일 집계 완료");
        }
    }

    private void giveLentTicket(User user, Item item) {
        ItemHistory reward = new ItemHistory(LocalDateTime.now(), null, user, item);
        itemHistoryRepository.save(reward);
        log.info("🎉 [Reward] {}님 지난달 50시간 달성! 대여권 지급 완료.", user.getName());
    }
}