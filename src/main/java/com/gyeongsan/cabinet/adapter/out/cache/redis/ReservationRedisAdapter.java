package com.gyeongsan.cabinet.adapter.out.cache.redis;

import com.gyeongsan.cabinet.domain.lent.model.ReservationOutcome;
import com.gyeongsan.cabinet.domain.lent.port.out.ReservationPort;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationRedisAdapter implements ReservationPort {

    private final StringRedisTemplate redisTemplate;

    private static final String CABINET_KEY_PREFIX = "cabinet:reservation:";
    private static final String USER_KEY_PREFIX = "user:reservation:";

    private static final long STATUS_RESERVED = 0L;
    private static final long STATUS_ALREADY_MINE = 1L;
    private static final long STATUS_TAKEN_BY_OTHER = 2L;

    private static final DefaultRedisScript<List> RESERVE_REPLACING_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    local owner = redis.call('GET', KEYS[1])
                    local old = redis.call('GET', KEYS[2])
                    if owner and owner ~= ARGV[1] then return {2, ''} end
                    if old and old == ARGV[2] and owner == ARGV[1] then return {1, old} end
                    if old and old ~= ARGV[2] then
                      local oldKey = ARGV[4] .. old
                      if redis.call('GET', oldKey) == ARGV[1] then redis.call('DEL', oldKey) end
                    end
                    redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[3])
                    redis.call('SET', KEYS[2], ARGV[2], 'EX', ARGV[3])
                    if old and old ~= ARGV[2] then return {0, old} end
                    return {0, ''}
                    """,
                    List.class);

    private static final DefaultRedisScript<String> CANCEL_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    local cancelled = ''
                    local mine = redis.call('GET', KEYS[1])
                    if mine then
                      local cabinetKey = ARGV[2] .. mine
                      if redis.call('GET', cabinetKey) == ARGV[1] then redis.call('DEL', cabinetKey) end
                      redis.call('DEL', KEYS[1])
                      cancelled = mine
                    end
                    if ARGV[3] ~= '' then
                      local rentedKey = ARGV[2] .. ARGV[3]
                      if redis.call('GET', rentedKey) == ARGV[1] then redis.call('DEL', rentedKey) end
                    end
                    return cancelled
                    """,
                    String.class);

    @Override
    public ReservationOutcome reserveReplacing(Integer visibleNum, Long userId, long ttlMinutes) {
        List<?> result =
                redisTemplate.execute(
                        RESERVE_REPLACING_SCRIPT,
                        List.of(CABINET_KEY_PREFIX + visibleNum, USER_KEY_PREFIX + userId),
                        userId.toString(),
                        visibleNum.toString(),
                        String.valueOf(TimeUnit.MINUTES.toSeconds(ttlMinutes)),
                        CABINET_KEY_PREFIX);
        if (result == null || result.size() < 2) {
            throw new IllegalStateException("예약 스크립트 실행 결과가 올바르지 않습니다.");
        }
        long status = ((Number) result.get(0)).longValue();
        String replaced = result.get(1) == null ? "" : result.get(1).toString();

        if (status == STATUS_ALREADY_MINE) {
            return new ReservationOutcome(ReservationOutcome.Status.ALREADY_MINE, null);
        }
        if (status == STATUS_TAKEN_BY_OTHER) {
            return new ReservationOutcome(ReservationOutcome.Status.TAKEN_BY_OTHER, null);
        }
        if (status != STATUS_RESERVED) {
            throw new IllegalStateException("알 수 없는 예약 결과입니다: " + status);
        }
        return new ReservationOutcome(
                ReservationOutcome.Status.RESERVED,
                replaced.isEmpty() ? null : Integer.valueOf(replaced));
    }

    @Override
    public Optional<Long> getReservedUserId(Integer visibleNum) {
        String value = redisTemplate.opsForValue().get(cabinetKey(visibleNum));
        return value != null ? Optional.of(Long.valueOf(value)) : Optional.empty();
    }

    @Override
    public Map<Integer, Long> getReservedUserIds(Collection<Integer> visibleNums) {
        if (visibleNums == null || visibleNums.isEmpty()) {
            return Map.of();
        }
        List<Integer> nums = List.copyOf(visibleNums);
        List<String> values =
                redisTemplate.opsForValue().multiGet(nums.stream().map(this::cabinetKey).toList());
        Map<Integer, Long> reserved = new HashMap<>();
        if (values == null) {
            return reserved;
        }
        for (int i = 0; i < nums.size() && i < values.size(); i++) {
            if (values.get(i) != null) {
                reserved.put(nums.get(i), Long.valueOf(values.get(i)));
            }
        }
        return reserved;
    }

    @Override
    public Optional<Long> getCabinetReservationTtlSeconds(Integer visibleNum) {
        Long ttl = redisTemplate.getExpire(cabinetKey(visibleNum), TimeUnit.SECONDS);
        return ttl != null && ttl >= 0 ? Optional.of(ttl) : Optional.empty();
    }

    private String cabinetKey(Integer visibleNum) {
        return CABINET_KEY_PREFIX + visibleNum;
    }

    @Override
    public Optional<Integer> getUserReservation(Long userId) {
        String value = redisTemplate.opsForValue().get(USER_KEY_PREFIX + userId);
        return value != null ? Optional.of(Integer.valueOf(value)) : Optional.empty();
    }

    @Override
    public Optional<Long> getUserReservationTtlSeconds(Long userId) {
        Long ttl = redisTemplate.getExpire(USER_KEY_PREFIX + userId, TimeUnit.SECONDS);
        return ttl != null && ttl >= 0 ? Optional.of(ttl) : Optional.empty();
    }

    @Override
    public void deleteReservation(Integer visibleNum, Long userId) {
        runCancel(userId, visibleNum.toString());
    }

    @Override
    public Optional<Integer> cancelUserReservation(Long userId) {
        String cancelled = runCancel(userId, "");
        return cancelled == null || cancelled.isEmpty()
                ? Optional.empty()
                : Optional.of(Integer.valueOf(cancelled));
    }

    private String runCancel(Long userId, String rentedVisibleNum) {
        return redisTemplate.execute(
                CANCEL_SCRIPT,
                List.of(USER_KEY_PREFIX + userId),
                userId.toString(),
                CABINET_KEY_PREFIX,
                rentedVisibleNum);
    }
}
