package com.skala.shopping.auth.internal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.skala.shopping.common.RateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

class RedisAuthenticationRateLimiterTests {
    @Test
    void rejectsRequestWhenSharedIpCounterExceedsLimit() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(101L);
        when(redis.getExpire(any(String.class))).thenReturn(30L);
        RedisAuthenticationRateLimiter limiter = new RedisAuthenticationRateLimiter(redis, new SecurityProperties());

        assertThrows(RateLimitExceededException.class,
                () -> limiter.checkLogin("203.0.113.10", "customer"));
    }
}
