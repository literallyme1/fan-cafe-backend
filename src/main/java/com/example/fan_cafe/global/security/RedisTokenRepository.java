package com.example.fan_cafe.global.security;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class RedisTokenRepository {

    private final StringRedisTemplate redisTemplate;

    @Value("{jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    public void save(Long userId, String refreshToken) {
        String key = "refresh:" + userId;
        Duration ttl = Duration.ofMillis(refreshTokenExpiration);
        redisTemplate.opsForValue().set(key, refreshToken, ttl);
    }

    public String find(Long userId) {
        return redisTemplate.opsForValue().get("refresh:" + userId);
    }

    public void delete(Long userId) {
        redisTemplate.delete("refresh:" + userId);
    }

}
