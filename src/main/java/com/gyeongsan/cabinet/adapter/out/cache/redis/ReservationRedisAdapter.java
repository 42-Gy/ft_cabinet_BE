package com.gyeongsan.cabinet.adapter.out.cache.redis;

import com.gyeongsan.cabinet.domain.lent.model.ReservationOutcome;
import com.gyeongsan.cabinet.domain.lent.port.out.ReservationPort;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * 예약은 두 키로 저장된다: cabinet:reservation:{번호} → 예약자, user:reservation:{userId} → 번호.
 *
 * <p>두 키가 어긋나면(한쪽만 남으면) 사용자에게는 보이지 않는 "유령 선점"이 생기므로, 두 키를 함께 바꾸는 동작은 모두 Lua 스크립트로 한 번에 실행한다. 스크립트는
 * 사물함 키를 이름으로 조립해 접근하므로 Redis 클러스터 모드에서는 쓸 수 없다(테스트/운영 모두 비클러스터).
 */
@Component
@RequiredArgsConstructor
public class ReservationRedisAdapter implements ReservationPort {

    private final StringRedisTemplate redisTemplate;

    private static final String CABINET_KEY_PREFIX = "cabinet:reservation:";
    private static final String USER_KEY_PREFIX = "user:reservation:";

    private static final long STATUS_RESERVED = 0L;
    private static final long STATUS_ALREADY_MINE = 1L;
    private static final long STATUS_TAKEN_BY_OTHER = 2L;

    /**
     * KEYS[1]=새 사물함 키, KEYS[2]=내 예약 키 / ARGV[1]=userId, ARGV[2]=새 번호, ARGV[3]=TTL(초), ARGV[4]=사물함 키
     * 접두어. 반환: {상태, 취소된 기존 번호 또는 ''}.
     */
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

    /**
     * KEYS[1]=내 예약 키 / ARGV[1]=userId, ARGV[2]=사물함 키 접두어, ARGV[3]=함께 정리할 사물함 번호 또는 ''. 내 예약이 가리키는
     * 사물함 키와 ARGV[3] 의 사물함 키는 예약자가 나일 때만 지운다. 반환: 취소된 내 예약 번호 또는 ''.
     */
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
        String value = redisTemplate.opsForValue().get(CABINET_KEY_PREFIX + visibleNum);
        return value != null ? Optional.of(Long.valueOf(value)) : Optional.empty();
    }

    @Override
    public Optional<Integer> getUserReservation(Long userId) {
        String value = redisTemplate.opsForValue().get(USER_KEY_PREFIX + userId);
        return value != null ? Optional.of(Integer.valueOf(value)) : Optional.empty();
    }

    @Override
    public Optional<Long> getUserReservationTtlSeconds(Long userId) {
        Long ttl = redisTemplate.getExpire(USER_KEY_PREFIX + userId, TimeUnit.SECONDS);
        // -2: 키 없음, -1: 만료 없음(예약 키에는 항상 TTL 이 있으므로 비정상). 둘 다 "남은 시간 없음".
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
