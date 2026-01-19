package com.example.fan_cafe.infrastructure.monitoring.health;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HealthCoreKeyInitializer {

    private final StringRedisTemplate redisTemplate;

    @PostConstruct
    public void init() {
        redisTemplate.opsForValue().set("health:core", "OK");
    }
}
