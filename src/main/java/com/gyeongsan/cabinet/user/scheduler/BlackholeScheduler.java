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

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Log4j2
public class BlackholeScheduler {

    private final UserRepository userRepository;
    private final LentFacadeService lentFacadeService;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 15 6 * * *")
    public void processBlackholedUsers() {
        log.info("블랙홀 대상 유저 체크 시작");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);

        List<User> blackholedUsers = userRepository.findAllBlackholedUsers(sevenDaysAgo);

        if (blackholedUsers.isEmpty()) {
            return;
        }

        for (User user : blackholedUsers) {
            try {
                lentFacadeService.processBlackholeReturn(user.getId());

                String message = String.format(
                        "[반납 보류] %s님은 블랙홀 진입으로 인해 사물함 사용 권한이 소멸되었습니다. " +
                                "내용물을 수거하고 앱에서 사진 촬영 후 직접 반납을 완료해주세요. " +
                                "미이행 시 패널티가 부과될 수 있습니다.",
                        user.getName());

                eventPublisher.publishEvent(new AlarmEvent(user.getName(), user.getEmail(), message));

                log.warn("{} 유저 블랙홀 발생 - 반납 보류 처리 및 알림 발행 완료", user.getName());

            } catch (Exception e) {
                log.error("{} 유저 블랙홀 처리 중 에러 발생: {}", user.getName(), e.getMessage());
            }
        }
    }
}