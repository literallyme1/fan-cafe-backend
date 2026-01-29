package com.example.fan_cafe.infrastructure.monitoring.health;

import com.example.fan_cafe.infrastructure.monitoring.HealthKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisCoreKeyHealthIndicator implements HealthIndicator {

    private final StringRedisTemplate redisTemplate;


    @Override
    public Health health() {
        try {

            //핵심 키 조회
            Boolean exists = redisTemplate.hasKey(HealthKeys.REDIS_CORE);

            //연결 0, key 0
            if (Boolean.TRUE.equals(exists)) {
                return Health.up() //health update
                        .withDetail("coreKey", "exists")
                        .build();
            }

            //연결 0, key x (메모리 부족 등)
            return Health.down()
                    .withDetail("reason", "CORE_KEY_NOT_FOUND") //json 필드 추가
                    .withDetail("coreKey", HealthKeys.REDIS_CORE)
                    .build();

        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("reason", "REDIS_ACCESS_FAILED")
                    .build();
        }
    }
}
