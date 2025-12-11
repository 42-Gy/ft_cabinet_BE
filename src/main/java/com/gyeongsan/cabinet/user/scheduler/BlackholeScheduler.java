package com.gyeongsan.cabinet.user.scheduler;

import com.gyeongsan.cabinet.alarm.dto.AlarmEvent; // 👈 추가
import com.gyeongsan.cabinet.lent.service.LentFacadeService;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher; // 👈 추가
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Log4j2
public class BlackholeScheduler {

    private final UserRepository userRepository;
    private final LentFacadeService lentFacadeService;
    private final ApplicationEventPublisher eventPublisher; // ⭕ 이벤트 발행기 주입

    /**
     * 매일 자정 0시 0분 0초에 실행 (블랙홀 강제 반납)
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void processBlackholedUsers() {
        log.info("⚫️ 블랙홀 자동 반납 처리 시작!");
        LocalDateTime now = LocalDateTime.now();

        // 1. 블랙홀에 빠진 유저 조회
        List<User> blackholedUsers = userRepository.findAllBlackholedUsers(now);

        if (blackholedUsers.isEmpty()) {
            log.info("⚫️ 처리할 블랙홀 유저 없음.");
            return;
        }

        for (User user : blackholedUsers) {
            try {
                // 2. 강제 반납 처리 시도
                lentFacadeService.endLentCabinet(user.getId());

                // 3. 알림 이벤트 발행 (비동기 처리 위임)
                String message = String.format("⚫️ *[블랙홀 진입]* %s님, 블랙홀 진입으로 인해 사물함이 자동 반납 처리되었습니다.", user.getName());

                // 👉 이벤트를 던집니다! (AlarmEventHandler가 받아서 처리함)
                eventPublisher.publishEvent(new AlarmEvent(user.getEmail(), message));

                log.warn("⚫️ {} 유저 강제 반납 및 알림 이벤트 발행 완료.", user.getName());

            } catch (IllegalArgumentException e) {
                // 이미 반납했거나, 대여 중인 사물함이 없는 경우 (정상 로그)
                log.info("⚫️ {} 유저는 처리할 대여 사물함이 없습니다.", user.getName());
            } catch (Exception e) {
                log.error("⚫️ {} 유저 반납 처리 중 심각한 에러 발생: {}", user.getName(), e.getMessage());
            }
        }
    }
}