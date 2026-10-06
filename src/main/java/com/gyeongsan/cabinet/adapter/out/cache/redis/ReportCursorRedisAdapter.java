package com.gyeongsan.cabinet.adapter.out.cache.redis;

import com.gyeongsan.cabinet.domain.alarm.port.out.ReportCursorPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 채널별 마지막 확인 ts 를 Redis 에 둔다. 만료는 두지 않는다. Redis 가 비워지면 커서가 없어져 "지금부터 다시 시작"으로 안전하게 동작한다(그 사이 글은
 * 놓치지만 과거 글 폭탄은 없다).
 */
@Component
@RequiredArgsConstructor
public class ReportCursorRedisAdapter implements ReportCursorPort {

    private static final String KEY_PREFIX = "slack:report:cursor:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public Optional<String> getCursor(String channelId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + channelId));
    }

    @Override
    public void saveCursor(String channelId, String ts) {
        redisTemplate.opsForValue().set(KEY_PREFIX + channelId, ts);
    }
}
