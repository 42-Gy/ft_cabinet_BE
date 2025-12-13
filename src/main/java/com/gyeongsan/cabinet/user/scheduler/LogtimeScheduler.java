package com.gyeongsan.cabinet.user.scheduler;

import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import com.gyeongsan.cabinet.utils.FtApiManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Log4j2
public class LogtimeScheduler {

    private final UserRepository userRepository;
    private final FtApiManager ftApiManager;

    private static final int COIN_PER_10_MIN = 1;

    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void rewardCoins() {
        log.info("로그타임 코인 정산 시작");

        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            try {
                int minutes = ftApiManager.getYesterdayLogtimeMinutes(user.getName());

                if (minutes > 0) {
                    long earnedCoin = (minutes / 10) * COIN_PER_10_MIN;

                    if (earnedCoin > 0) {
                        user.addCoin(earnedCoin);
                        log.info("{}: {}분 공부 -> {} 코인 지급 완료", user.getName(), minutes, earnedCoin);
                    }
                }

                Thread.sleep(500);

            } catch (Exception e) {
                log.error("{} 정산 실패: {}", user.getName(), e.getMessage());
            }
        }

        log.info("코인 정산 종료");
    }
}
