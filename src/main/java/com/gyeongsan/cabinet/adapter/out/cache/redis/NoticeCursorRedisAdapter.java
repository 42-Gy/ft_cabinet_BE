package com.gyeongsan.cabinet.adapter.out.cache.redis;

import com.gyeongsan.cabinet.domain.kakaonotify.port.out.NoticeCursorPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 공지 채널별 마지막 확인 ts. 오류제보 커서({@code slack:report:cursor:})와 키 접두사를 달리해, 같은 채널을 두 기능에 써도 서로의 위치를 덮어쓰지
 * 않는다. 만료는 두지 않는다. Redis 가 비워지면 "지금부터 다시 시작"으로 안전하게 동작한다(그 사이 공지는 놓치지만 과거 공지 폭탄은 없다).
 */
@Component
@RequiredArgsConstructor
public class NoticeCursorRedisAdapter implements NoticeCursorPort {

    private static final String KEY_PREFIX = "slack:notice:cursor:";

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
