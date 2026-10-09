package com.gyeongsan.cabinet.adapter.in.scheduler.user;

import com.gyeongsan.cabinet.config.RedisStreamConfig;
import com.gyeongsan.cabinet.domain.item.model.Item;
import com.gyeongsan.cabinet.domain.item.port.out.ItemRepositoryPort;
import com.gyeongsan.cabinet.domain.lent.port.out.FtApiPort;
import com.gyeongsan.cabinet.domain.user.model.FtGradeResolver;
import com.gyeongsan.cabinet.domain.user.model.FtGradeSnapshot;
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
                if (isPayDay) {
                    refreshGradeIfNeeded(userId, intraId, totalMinutes);
                }
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

    /**
     * grade 는 로그인 때만 갱신되므로, 새 기준의 영향을 받는 사용자(트센 기준 이상, 일반 기준 미만의 비-트센)만 지급일에 다시 조회한다. 조회나 저장에 실패해도
     * 지급 처리는 막지 않고, 저장된 값을 그대로 쓴다(트센으로 확인되지 않으면 일반 기준이라 대여권이 더 나가지는 않는다).
     */
    private void refreshGradeIfNeeded(Long userId, String intraId, int totalMinutes) {
        try {
            if (!userUseCase.needsGradeRefresh(userId, totalMinutes)) {
                return;
            }
            FtGradeSnapshot snapshot = FtGradeResolver.resolve(ftApiPort.getCursusEntries(intraId));
            if (!snapshot.parsed()) {
                log.warn("⚠️ [Consumer] {} grade 재조회 실패. 저장된 값을 유지합니다.", intraId);
                return;
            }
            userUseCase.updateFtGrade(userId, snapshot);
        } catch (Exception e) {
            log.warn("⚠️ [Consumer] {} grade 재조회 중 오류. 저장된 값을 유지합니다: {}", intraId, e.getMessage());
        }
    }
}
