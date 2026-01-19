package com.example.fan_cafe.infrastructure.monitoring.health;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class HealthCoreKeyInitializer {

    private final StringRedisTemplate redisTemplate;

    @PostConstruct
    public void init() {
        try {
            redisTemplate.opsForValue().set("health:core", "OK");
        } catch (Exception e) {
            // health 보조 로직 실패는 앱 기동을 막지 않음
            // 로그만 남김
            log.warn("[HEALTH INIT] Redis unavailable, core key not set", e);
        }
    }
}
