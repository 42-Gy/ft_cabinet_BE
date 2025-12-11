package com.gyeongsan.cabinet.alarm;

import com.gyeongsan.cabinet.alarm.dto.AlarmEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class AlarmEventHandler {

    private final SlackBotService slackBotService;

    @Async // 👈 [핵심] 이 메서드는 별도 스레드(백그라운드)에서 실행됩니다!
    @EventListener // 👈 누군가 AlarmEvent를 날리면 여기서 받습니다.
    public void handleAlarmEvent(AlarmEvent event) {
        log.info("📨 [비동기] 알림 이벤트 수신! 대상: {}", event.getEmail());

        // 실제 느린 작업(슬랙 전송)은 여기서 수행
        slackBotService.sendDm(event.getEmail(), event.getMessage());
    }
}