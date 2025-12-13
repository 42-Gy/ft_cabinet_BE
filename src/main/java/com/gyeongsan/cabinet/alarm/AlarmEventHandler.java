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

    @Async
    @EventListener
    public void handleAlarmEvent(AlarmEvent event) {
        log.info("📨 [비동기] 알림 이벤트 수신! 대상: {}", event.getEmail());
        slackBotService.sendDm(event.getEmail(), event.getMessage());
    }
}
