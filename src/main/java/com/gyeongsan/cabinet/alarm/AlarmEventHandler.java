package com.gyeongsan.cabinet.alarm;

import com.gyeongsan.cabinet.alarm.dto.AlarmEvent;
import com.gyeongsan.cabinet.config.RedisStreamConfig;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class AlarmEventHandler {

    private final RedisTemplate<String, Object> redisTemplate;

    @org.springframework.transaction.event.TransactionalEventListener(
            phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
    public void handleAlarmEvent(AlarmEvent event) {
        log.info("📨 [Publisher] 트랜잭션 커밋 후 슬랙 알림 이벤트를 Redis Stream으로 전송: {}", event.getIntraId());

        Map<String, String> streamData = new HashMap<>();
        streamData.put("intraId", event.getIntraId());
        if (event.getEmail() != null) {
            streamData.put("email", event.getEmail());
        }
        streamData.put("message", event.getMessage());

        redisTemplate.opsForStream().add(RedisStreamConfig.SLACK_STREAM_KEY, streamData);
    }
}
