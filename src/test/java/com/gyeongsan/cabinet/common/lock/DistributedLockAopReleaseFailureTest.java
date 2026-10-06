package com.gyeongsan.cabinet.common.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

class DistributedLockAopReleaseFailureTest {

    public static class Target {
        final AtomicInteger calls = new AtomicInteger();

        @DistributedLock(key = "k", identifier = "#id")
        public String run(Long id) {
            calls.incrementAndGet();
            return "done:" + id;
        }
    }

    @SuppressWarnings("unchecked")
    private Target proxyWith(StringRedisTemplate template, Target target) {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new DistributedLockAop(template));
        return factory.getProxy();
    }

    @Test
    @DisplayName("락 해제 중 Redis 오류가 나도 이미 끝난 업무 결과는 그대로 돌려준다")
    void releaseFailureDoesNotMaskResult() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        when(template.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new IllegalStateException("redis down"));
        Target target = new Target();

        assertThat(proxyWith(template, target).run(7L)).isEqualTo("done:7");
        assertThat(target.calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("해제 스크립트가 0(이미 만료/남의 락)을 돌려줘도 결과에는 영향이 없다")
    void releaseNotOwnedIsHarmless() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        when(template.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(0L);

        assertThat(proxyWith(template, new Target()).run(7L)).isEqualTo("done:7");
    }
}
