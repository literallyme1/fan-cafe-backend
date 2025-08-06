package com.example.fan_cafe.auth.infrastructure;


import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class PasswordResetTokenRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PREFIX = "reset-password:";

    public void save(String token, PasswordResetPayload payload, Duration ttl) {
        redisTemplate.opsForValue().set(PREFIX + token, payload, ttl);
    }

    public PasswordResetPayload find(String token){
        return (PasswordResetPayload) redisTemplate.opsForValue().get(PREFIX + token);
    }

    public void delete(String token) {
        redisTemplate.delete(PREFIX + token);
    }
}
