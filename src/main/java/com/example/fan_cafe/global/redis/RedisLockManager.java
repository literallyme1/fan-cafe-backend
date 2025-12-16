package com.example.fan_cafe.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisLockManager {

    private final StringRedisTemplate stringRedisTemplate;

    public boolean tryLock(String key, long ttlSeconds) {
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "lock", Duration.ofSeconds(ttlSeconds));
        return Boolean.TRUE.equals(success);
    }

    public void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
