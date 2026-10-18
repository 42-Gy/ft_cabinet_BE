package com.gyeongsan.cabinet.common.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gyeongsan.cabinet.global.exception.ErrorCode;
import com.gyeongsan.cabinet.global.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class DistributedLockAopArgBindingTest {

    static class Target {
        @DistributedLock(key = "test-key", identifier = "#id")
        public String run(Long id) {
            return "ok:" + id;
        }

        @DistributedLock(key = "test-key", identifier = "#id", waitTime = 1)
        public String runWithShortWait(Long id) {
            return "ok:" + id;
        }
    }

    @Test
    void proxiedMethodRunsWithoutAspectJArgBindingError() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);

        DistributedLockAop aspect = new DistributedLockAop(redisTemplate);

        AspectJProxyFactory factory = new AspectJProxyFactory(new Target());
        factory.addAspect(aspect);
        Target proxy = factory.getProxy();

        String result = proxy.run(42L);

        assertThat(result).isEqualTo("ok:42");
    }

    @Test
    void lockContention_throwsServiceExceptionWithLockErrorCode() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(false);

        DistributedLockAop aspect = new DistributedLockAop(redisTemplate);

        AspectJProxyFactory factory = new AspectJProxyFactory(new Target());
        factory.addAspect(aspect);
        Target proxy = factory.getProxy();

        assertThatThrownBy(() -> proxy.runWithShortWait(42L))
                .isInstanceOf(ServiceException.class)
                .extracting(e -> ((ServiceException) e).getErrorCode())
                .isEqualTo(ErrorCode.REQUEST_IN_PROGRESS);
    }
}
