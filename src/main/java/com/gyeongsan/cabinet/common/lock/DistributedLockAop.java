package com.gyeongsan.cabinet.common.lock;

import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Log4j2
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class DistributedLockAop {

    /** 값이 내 토큰일 때만 지운다. 확인과 삭제가 한 번에 일어나야 해서 Lua 로 실행한다(키가 하나라 클러스터에서도 동작). */
    private static final RedisScript<Long> RELEASE_SCRIPT =
            new DefaultRedisScript<>(
                    "if redis.call('GET', KEYS[1]) == ARGV[1] then"
                            + " return redis.call('DEL', KEYS[1]) else return 0 end",
                    Long.class);

    private final StringRedisTemplate redisTemplate;
    private final ParameterNameDiscoverer parameterNameDiscoverer =
            new DefaultParameterNameDiscoverer();
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    @Around(value = "@annotation(distributedLock)", argNames = "distributedLock")
    public Object lock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock)
            throws Throwable {
        String key = distributedLock.key();
        String identifier = distributedLock.identifier();

        if (identifier != null && !identifier.isEmpty()) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            EvaluationContext context = new StandardEvaluationContext();

            String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
            Object[] args = joinPoint.getArgs();

            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length; i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }

            Object evaluatedId = expressionParser.parseExpression(identifier).getValue(context);
            if (evaluatedId != null) {
                key = key + ":" + evaluatedId.toString();
            }
        }

        long waitTime = distributedLock.waitTime();
        long leaseTime = distributedLock.leaseTime();

        // 락을 잡은 요청만 아는 값. lease 가 만료돼 다른 요청이 같은 키를 잡았을 때, 먼저 끝난 요청이 남의 락을 지우지 않게 한다.
        String token = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        boolean isLocked = false;

        try {
            while (System.currentTimeMillis() - startTime < waitTime * 1000) {
                Boolean success =
                        redisTemplate
                                .opsForValue()
                                .setIfAbsent(key, token, leaseTime, TimeUnit.SECONDS);
                if (Boolean.TRUE.equals(success)) {
                    isLocked = true;
                    break;
                }
                Thread.sleep(100);
            }

            if (!isLocked) {
                log.warn("[DistributedLock] 락 획득 실패 - Key: {}", key);
                throw new ServiceException(ErrorCode.REQUEST_IN_PROGRESS);
            }

            return joinPoint.proceed();
        } finally {
            if (isLocked) {
                release(key, token, leaseTime);
            }
        }
    }

    /** 내 락일 때만 해제한다. 해제 실패가 이미 끝난 업무 결과를 덮어쓰지 않도록 예외는 삼키고 기록만 한다(락은 lease 로 만료된다). */
    private void release(String key, String token, long leaseTime) {
        try {
            Long deleted = redisTemplate.execute(RELEASE_SCRIPT, List.of(key), token);
            if (deleted == null || deleted == 0L) {
                log.warn(
                        "[DistributedLock] 락이 이미 만료되었거나 다른 요청이 가져가 해제하지 않음 - Key: {}"
                                + " (처리 시간이 leaseTime {}초를 넘겼을 수 있음)",
                        key,
                        leaseTime);
            }
        } catch (RuntimeException e) {
            log.error(
                    "[DistributedLock] 락 해제 실패(lease 만료로 풀림) - Key: {}, 원인: {}", key, e.toString());
        }
    }
}
