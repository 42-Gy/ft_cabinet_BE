package com.gyeongsan.cabinet.user.scheduler;

import com.gyeongsan.cabinet.item.domain.Item;
import com.gyeongsan.cabinet.item.domain.ItemType;
import com.gyeongsan.cabinet.item.repository.ItemRepository;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import com.gyeongsan.cabinet.user.service.UserService;
import com.gyeongsan.cabinet.utils.FtApiManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Log4j2
public class LogtimeScheduler {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final FtApiManager ftApiManager;
    private final UserService userService;

    @Scheduled(cron = "0 0 6 * * *")
    public void processDailyLogtime() {
        log.info("📅 [Daily] 로그타임 집계 시작 (누적 덮어쓰기 모드)");

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

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth;
        LocalDateTime endOfYesterday;

        if (isPayDay) {
            startOfMonth = now.minusMonths(1).withDayOfMonth(1).toLocalDate().atStartOfDay();
            endOfYesterday = now.minusDays(1).toLocalDate().atTime(23, 59, 59);
        } else {
            startOfMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
            endOfYesterday = now.minusDays(1).toLocalDate().atTime(23, 59, 59);
        }

        log.info("조회 기간: {} ~ {}", startOfMonth, endOfYesterday);

        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            try {
                int totalMinutes = ftApiManager.getLogtimeBetween(
                        user.getName(),
                        startOfMonth,
                        endOfYesterday
                );

                userService.processLogtimeTransaction(
                        user.getId(),
                        lentTicketItem,
                        totalMinutes,
                        isPayDay
                );

                Thread.sleep(50);

            } catch (Exception e) {
                log.error("{} 로그타임 처리 중 에러: {}", user.getName(), e.getMessage());
            }
        }

        if (isPayDay) {
            log.info("✅ [Monthly] 월간 보상 지급 및 초기화 완료");
        } else {
            log.info("✅ [Daily] 일일 집계(동기화) 완료");
        }
    }
}
