package com.example.fan_cafe.infrastructure.monitoring.detect;

import com.example.fan_cafe.infrastructure.monitoring.HealthKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisHealthProbe {

    private final StringRedisTemplate redisTemplate;

    public ComponentHealthStatus check() {
        try {
            Boolean exists = redisTemplate.hasKey(HealthKeys.REDIS_CORE);

        } catch (Exception e) {
            return ComponentHealthStatus.DOWN;
        }
    }
}
