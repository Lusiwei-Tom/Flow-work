package com.flowwork.modules.project.service;

import com.flowwork.common.exception.BusinessException;
import com.flowwork.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 任务编辑分布式锁：Redis SET NX PX
 * key: flowwork:lock:task:{taskId}，value = userId，TTL 15s
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskLockService {

    private static final String LOCK_PREFIX = "flowwork:lock:task:";
    private static final long TTL_SECONDS = 15;

    private final StringRedisTemplate redis;

    public boolean tryLock(Long taskId, Long userId) {
        Boolean ok = redis.opsForValue().setIfAbsent(
                LOCK_PREFIX + taskId, String.valueOf(userId), TTL_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(ok);
    }

    public void unlock(Long taskId, Long userId) {
        String key = LOCK_PREFIX + taskId;
        String holder = redis.opsForValue().get(key);
        if (String.valueOf(userId).equals(holder)) {
            redis.delete(key);
        }
    }

    /** 加锁执行模板 */
    public <T> T withLock(Long taskId, Long userId, Supplier<T> action) {
        if (!tryLock(taskId, userId)) {
            throw new BusinessException(ResultCode.LOCK_CONFLICT);
        }
        try {
            return action.get();
        } finally {
            unlock(taskId, userId);
        }
    }
}
