package com.gyeongsan.cabinet.user.scheduler;

import com.gyeongsan.cabinet.domain.item.port.out.ItemRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.FtApiPort;
import com.gyeongsan.cabinet.domain.user.port.in.UserUseCase;
import com.gyeongsan.cabinet.domain.user.port.out.UserRepositoryPort;
import com.gyeongsan.cabinet.item.domain.Item;
import com.gyeongsan.cabinet.item.domain.ItemType;
import com.gyeongsan.cabinet.user.domain.User;
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

    private final UserRepositoryPort userRepository;
    private final ItemRepositoryPort itemRepository;
    private final FtApiPort ftApiPort;
    private final UserUseCase userUseCase;

    @Scheduled(cron = "0 0 1 * * *")
    public void processDailyLogtime() {
        log.info("[Daily] 로그타임 집계 시작 (병렬 처리 모드)");

        boolean isPayDay = LocalDate.now().getDayOfMonth() == 1;

        Item lentTicketItem = null;
        if (isPayDay) {
            lentTicketItem = itemRepository.findAll().stream()
                    .filter(i -> i.getType() == ItemType.LENT)
                    .findFirst()
                    .orElse(null);

            if (lentTicketItem == null) {
                log.error("[Error] 보상 지급 실패: LENT 아이템이 DB에 없습니다.");
                return;
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

        Item finalRewardItem = lentTicketItem;

        for (User user : allUsers) {
            try {
                processUserLogtime(user, startOfMonth, endOfYesterday, finalRewardItem, isPayDay);
                Thread.sleep(600);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("스케줄러 인터럽트 발생");
                break;
            } catch (Exception e) {
                log.error("{} 로그타임 처리 중 에러: {}", user.getName(), e.getMessage());
            }
        }

        if (isPayDay) {
            log.info("[Monthly] 월간 보상 지급 및 초기화 완료");
        } else {
            log.info("[Daily] 일일 집계(동기화) 완료");
        }
    }

    private void processUserLogtime(User user, LocalDateTime start, LocalDateTime end, Item rewardItem,
            boolean isPayDay) {
        int totalMinutes = ftApiPort.getLogtimeBetween(
                user.getName(),
                start,
                end);

        userUseCase.processLogtimeTransaction(
                user.getId(),
                rewardItem,
                totalMinutes,
                isPayDay);
    }
}