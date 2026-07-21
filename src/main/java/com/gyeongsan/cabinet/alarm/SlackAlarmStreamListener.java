package com.gyeongsan.cabinet.alarm;

import com.gyeongsan.cabinet.config.RedisStreamConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Log4j2
public class SlackAlarmStreamListener implements StreamListener<String, MapRecord<String, String, String>> {

    private final SlackBotService slackBotService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        try {
            Map<String, String> value = message.getValue();
            String intraId = value.get("intraId");
            String text = value.get("message");

            log.info("📥 [Consumer] Redis Stream에서 슬랙 알림 메시지 수신: {}", intraId);
            
            // 실제 슬랙 알림 발송 처리
            slackBotService.sendDm(intraId, text);

            // 처리 성공 시 스트림 그룹에 처리 완료(ACK) 보고
            redisTemplate.opsForStream().acknowledge(
                    RedisStreamConfig.CONSUMER_GROUP_NAME, message);
                    
            // 완료된 메시지는 메모리 정리를 위해 삭제 처리 (선택사항)
            redisTemplate.opsForStream().delete(message);
            
        } catch (Exception e) {
            log.error("🚨 [Consumer] 슬랙 알림 처리 중 에러 발생: {}", e.getMessage());
        }
    }
}
