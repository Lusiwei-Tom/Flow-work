package com.flowwork.modules.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * AI 接口限流：Redis INCR 滑动固定窗口
 * 默认每用户每分钟 10 次
 */
@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    private final StringRedisTemplate redis;
    private static final String PREFIX = "flowwork:rate:ai:";

    public boolean isAllowed(Long userId, int limit, int windowSeconds) {
        String key = PREFIX + userId;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
        return count == null || count <= limit;
    }
}
