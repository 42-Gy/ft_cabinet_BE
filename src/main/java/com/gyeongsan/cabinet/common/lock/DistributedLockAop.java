package com.gyeongsan.cabinet.common.lock;

import java.lang.reflect.Method;
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
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Log4j2
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DistributedLockAop {

    private final StringRedisTemplate redisTemplate;
    private final ParameterNameDiscoverer parameterNameDiscoverer =
            new DefaultParameterNameDiscoverer();
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    @Around("@annotation(distributedLock)")
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

        long startTime = System.currentTimeMillis();
        boolean isLocked = false;

        try {
            while (System.currentTimeMillis() - startTime < waitTime * 1000) {
                Boolean success =
                        redisTemplate
                                .opsForValue()
                                .setIfAbsent(key, "locked", leaseTime, TimeUnit.SECONDS);
                if (Boolean.TRUE.equals(success)) {
                    isLocked = true;
                    break;
                }
                Thread.sleep(100);
            }

            if (!isLocked) {
                log.warn("[DistributedLock] 락 획득 실패 - Key: {}", key);
                throw new IllegalStateException("현재 처리 중인 요청입니다. 잠시 후 다시 시도해주세요.");
            }

            return joinPoint.proceed();
        } finally {
            if (isLocked) {
                redisTemplate.delete(key);
            }
        }
    }
}
