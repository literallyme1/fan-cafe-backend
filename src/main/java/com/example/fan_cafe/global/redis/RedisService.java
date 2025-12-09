package com.example.fan_cafe.global.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisService {

    private final StringRedisTemplate stringRedisTemplate;

    public void set(String key, String value, Duration ttl) {
        stringRedisTemplate.opsForValue().set(key, value, ttl);
    }

    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    public int getInt(String key) {
        try {
            String v = stringRedisTemplate.opsForValue().get(key);
            return v == null ? 0 : Integer.parseInt(v);
        } catch (Exception e) {
            log.warn("[CACHE GET ERROR] key={}", key);
            return 0;
        }
    }
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    public void increaseCount(String key) {
        try {
            stringRedisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.warn("[CACHE INCR ERROR] key={} not increased", key, e);
        }
    }
}
