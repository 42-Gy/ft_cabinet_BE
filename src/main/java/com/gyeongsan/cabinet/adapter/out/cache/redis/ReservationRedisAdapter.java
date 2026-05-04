package com.gyeongsan.cabinet.adapter.out.cache.redis;

import com.gyeongsan.cabinet.domain.lent.port.out.ReservationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class ReservationRedisAdapter implements ReservationPort {

    private final StringRedisTemplate redisTemplate;

    private static final String CABINET_KEY_PREFIX = "cabinet:reservation:";
    private static final String USER_KEY_PREFIX = "user:reservation:";

    @Override
    public void reserve(Integer visibleNum, Long userId, long ttlMinutes) {
        String cabinetKey = CABINET_KEY_PREFIX + visibleNum;
        String userKey = USER_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(cabinetKey, userId.toString(), ttlMinutes, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(userKey, visibleNum.toString(), ttlMinutes, TimeUnit.MINUTES);
    }

    @Override
    public Optional<Long> getReservedUserId(Integer visibleNum) {
        String value = redisTemplate.opsForValue().get(CABINET_KEY_PREFIX + visibleNum);
        return value != null ? Optional.of(Long.valueOf(value)) : Optional.empty();
    }

    @Override
    public Optional<Integer> getUserReservation(Long userId) {
        String value = redisTemplate.opsForValue().get(USER_KEY_PREFIX + userId);
        return value != null ? Optional.of(Integer.valueOf(value)) : Optional.empty();
    }

    @Override
    public void deleteReservation(Integer visibleNum, Long userId) {
        redisTemplate.delete(CABINET_KEY_PREFIX + visibleNum);
        redisTemplate.delete(USER_KEY_PREFIX + userId);
    }
}
