package com.gyeongsan.cabinet.adapter.in.scheduler.user;

import com.gyeongsan.cabinet.config.RedisStreamConfig;
import com.gyeongsan.cabinet.domain.item.model.Item;
import com.gyeongsan.cabinet.domain.item.port.out.ItemRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.FtApiPort;
import com.gyeongsan.cabinet.domain.user.port.in.UserUseCase;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class LogtimeStreamListener
        implements StreamListener<String, MapRecord<String, String, String>> {

    private final FtApiPort ftApiPort;
    private final UserUseCase userUseCase;
    private final ItemRepositoryPort itemRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        try {
            Map<String, String> value = message.getValue();
            Long userId = Long.parseLong(value.get("userId"));
            String intraId = value.get("intraId");
            LocalDateTime start = LocalDateTime.parse(value.get("start"));
            LocalDateTime end = LocalDateTime.parse(value.get("end"));
            boolean isPayDay = Boolean.parseBoolean(value.get("isPayDay"));

            Item rewardItem = null;
            if (value.containsKey("rewardItemId")) {
                Long itemId = Long.parseLong(value.get("rewardItemId"));
                rewardItem = itemRepository.findById(itemId).orElse(null);
            }

            log.info("📥 [Consumer] Redis Stream에서 로그타임 동기화 이벤트 수신: {}", intraId);

            int totalMinutes = ftApiPort.getLogtimeBetween(intraId, start, end);

            if (totalMinutes < 0) {
                log.warn("⚠️ [Consumer] {} 로그타임 API 호출 실패. 기존 학습시간을 유지하고 건너뜁니다.", intraId);
            } else {
                userUseCase.processLogtimeTransaction(userId, rewardItem, totalMinutes, isPayDay);
            }

            Thread.sleep(600);

            redisTemplate
                    .opsForStream()
                    .acknowledge(RedisStreamConfig.CONSUMER_GROUP_NAME, message);
            redisTemplate.opsForStream().delete(message);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("🚨 [Consumer] 로그타임 동기화 중 인터럽트 발생: {}", e.getMessage());
        } catch (Exception e) {
            log.error("🚨 [Consumer] 로그타임 동기화 중 에러 발생 (ACK 처리 후 무시): {}", e.getMessage(), e);
            try {
                // 예외 발생 시에도 Pending에 계속 남지 않도록 ACK 처리 (일종의 자동 버리기)
                redisTemplate
                        .opsForStream()
                        .acknowledge(RedisStreamConfig.CONSUMER_GROUP_NAME, message);
                redisTemplate.opsForStream().delete(message);
            } catch (Exception ackEx) {
                log.error("🚨 [Consumer] ACK 처리 중 추가 에러 발생: {}", ackEx.getMessage());
            }
        }
    }
}
