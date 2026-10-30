package com.gyeongsan.cabinet.adapter.in.scheduler.stream;

import com.gyeongsan.cabinet.adapter.in.scheduler.user.LogtimeStreamListener;
import com.gyeongsan.cabinet.alarm.SlackAlarmStreamListener;
import com.gyeongsan.cabinet.config.RedisStreamConfig;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class PendingStreamReclaimer {

    static final Duration MIN_IDLE = Duration.ofMinutes(60);
    private static final long MAX_RECLAIM_COUNT = 300;

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisStreamConfig redisStreamConfig;
    private final SlackAlarmStreamListener slackAlarmStreamListener;
    private final LogtimeStreamListener logtimeStreamListener;

    @Scheduled(initialDelay = 60_000, fixedDelay = 600_000)
    public void reclaimPendingMessages() {
        reclaim(RedisStreamConfig.SLACK_STREAM_KEY, slackAlarmStreamListener);
        reclaim(RedisStreamConfig.LOGTIME_STREAM_KEY, logtimeStreamListener);
    }

    void reclaim(
            String streamKey, StreamListener<String, MapRecord<String, String, String>> listener) {
        try {
            StreamOperations<String, String, String> streamOps = stringRedisTemplate.opsForStream();

            RecordId[] idleIds =
                    streamOps
                            .pending(
                                    streamKey,
                                    RedisStreamConfig.CONSUMER_GROUP_NAME,
                                    Range.unbounded(),
                                    MAX_RECLAIM_COUNT)
                            .stream()
                            .filter(
                                    p ->
                                            p.getElapsedTimeSinceLastDelivery().compareTo(MIN_IDLE)
                                                    >= 0)
                            .map(PendingMessage::getId)
                            .toArray(RecordId[]::new);

            if (idleIds.length == 0) {
                return;
            }

            List<MapRecord<String, String, String>> claimed =
                    streamOps.claim(
                            streamKey,
                            RedisStreamConfig.CONSUMER_GROUP_NAME,
                            redisStreamConfig.getConsumerName(),
                            MIN_IDLE,
                            idleIds);

            log.warn("♻️ [Reclaim] {} 에서 방치된 메시지 {}건을 회수해 재처리합니다.", streamKey, claimed.size());
            claimed.forEach(listener::onMessage);
        } catch (Exception e) {
            log.error("🚨 [Reclaim] {} 방치 메시지 회수 실패: {}", streamKey, e.getMessage());
        }
    }
}
