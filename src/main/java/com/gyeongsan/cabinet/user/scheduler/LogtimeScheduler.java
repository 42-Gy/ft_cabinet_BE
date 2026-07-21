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
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
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
    @SchedulerLock(name = "processDailyLogtimeTask", lockAtMostFor = "60m", lockAtLeastFor = "5m")
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
                // 기존 동기 처리 및 Thread.sleep(600) 제거
                // 대신 Redis Stream 에 이벤트를 던지고 백그라운드 워커가 처리하도록 위임
                java.util.Map<String, String> streamData = new java.util.HashMap<>();
                streamData.put("userId", String.valueOf(user.getId()));
                streamData.put("intraId", user.getName());
                streamData.put("start", startOfMonth.toString());
                streamData.put("end", endOfYesterday.toString());
                streamData.put("isPayDay", String.valueOf(isPayDay));
                if (finalRewardItem != null) {
                    streamData.put("rewardItemId", String.valueOf(finalRewardItem.getId()));
                }

                org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate = 
                    com.gyeongsan.cabinet.global.utils.SpringContext.getBean("redisTemplate", org.springframework.data.redis.core.RedisTemplate.class);
                
                redisTemplate.opsForStream().add(com.gyeongsan.cabinet.config.RedisStreamConfig.LOGTIME_STREAM_KEY, streamData);
                
            } catch (Exception e) {
                log.error("{} 로그타임 처리 발행 중 에러: {}", user.getName(), e.getMessage());
            }
        }

        if (isPayDay) {
            log.info("[Monthly] 월간 보상 지급 및 초기화 (이벤트 발행 완료)");
        } else {
            log.info("[Daily] 일일 집계 동기화 (이벤트 발행 완료)");
        }
    }
}