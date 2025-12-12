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

    // 💰 코인 지급 비율: 10분당 1코인 (예시)
    private static final int COIN_PER_10_MIN = 1;

    /**
     * 매일 자정(00:00:00)에 실행
     * (테스트를 위해 지금은 1분마다 실행되게 해둠: "0 * * * * *")
     */
    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void rewardCoins() {
        log.info("💸 로그타임 코인 정산 시작!");

        // 1. 모든 유저 가져오기
        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            try {
                // 2. 42 API로 어제 공부 시간 조회 (분 단위)
                // (테스트를 위해 '어제'가 아니라 최근 접속 기록을 가져오게 로직이 되어 있습니다)
                int minutes = ftApiManager.getYesterdayLogtimeMinutes(user.getName());

                if (minutes > 0) {
                    // 3. 코인 계산 (10분당 1코인)
                    long earnedCoin = (minutes / 10) * COIN_PER_10_MIN;

                    if (earnedCoin > 0) {
                        user.addCoin(earnedCoin);
                        log.info(" - {}: {}분 공부 -> {} 코인 지급 완료!", user.getName(), minutes, earnedCoin);
                    }
                }

                // ⚠️ API 호출 너무 빠르면 42 서버가 싫어함 (0.5초 휴식)
                Thread.sleep(500);

            } catch (Exception e) {
                log.error("{} 정산 실패: {}", user.getName(), e.getMessage());
            }
        }

        log.info("💸 코인 정산 종료.");
    }
}