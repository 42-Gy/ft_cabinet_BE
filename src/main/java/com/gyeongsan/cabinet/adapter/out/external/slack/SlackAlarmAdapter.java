package com.gyeongsan.cabinet.adapter.out.external.slack;

import com.gyeongsan.cabinet.alarm.SlackBotService;
import com.gyeongsan.cabinet.domain.alarm.port.out.AlarmPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SlackAlarmAdapter implements AlarmPort {

    private final SlackBotService slackBotService;

    @Override
    public void sendDm(String intraId, String message) {
        slackBotService.sendDm(intraId, message);
    }
}
