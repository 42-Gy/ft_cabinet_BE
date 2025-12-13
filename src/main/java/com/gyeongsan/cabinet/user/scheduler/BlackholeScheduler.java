package com.gyeongsan.cabinet.user.scheduler;

import com.gyeongsan.cabinet.alarm.dto.AlarmEvent;
import com.gyeongsan.cabinet.lent.service.LentFacadeService;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void processBlackholedUsers() {
        log.info("블랙홀 자동 반납 처리 시작");
        LocalDateTime now = LocalDateTime.now();

        List<User> blackholedUsers = userRepository.findAllBlackholedUsers(now);

        if (blackholedUsers.isEmpty()) {
            log.info("처리할 블랙홀 유저 없음");
            return;
        }

        for (User user : blackholedUsers) {
            try {
                lentFacadeService.endLentCabinet(user.getId());

                String message = String.format(
                        "[블랙홀 진입] %s님, 블랙홀 진입으로 인해 사물함이 자동 반납 처리되었습니다.",
                        user.getName()
                );

                eventPublisher.publishEvent(new AlarmEvent(user.getEmail(), message));

                log.warn("{} 유저 강제 반납 및 알림 이벤트 발행 완료", user.getName());

            } catch (IllegalArgumentException e) {
                log.info("{} 유저는 처리할 대여 사물함이 없습니다.", user.getName());
            } catch (Exception e) {
                log.error("{} 유저 반납 처리 중 에러 발생: {}", user.getName(), e.getMessage());
            }
        }
    }
}
