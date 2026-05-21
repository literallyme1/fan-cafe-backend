package com.example.fan_cafe.outbox.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Outbox 컨슈머 idempotency용 read-through 캐시.
 * <p>최종 정합성은 {@code processed_events} UNIQUE에 두고, Redis는 조회 비용·DB 부하를 줄이는 용도다.
 * 키 형식: {@code processed:{eventId}:{consumerType}}
 */
@Component
public class ProcessedEventRedisCache {

    private static final String VALUE_MARKER = "1";

    private final StringRedisTemplate stringRedisTemplate;
    private final long ttlHours;

    public ProcessedEventRedisCache(
            StringRedisTemplate stringRedisTemplate,
            @Value("${outbox.idempotency.redis-ttl-hours:12}") long ttlHours
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.ttlHours = ttlHours;
    }

    /** Redis 키 문자열 생성 (설정/테스트에서 참조할 때 사용). */
    public String buildKey(String eventId, String consumerType) {
        return "processed:" + eventId + ":" + consumerType;
    }

    /** 캐시 히트 여부(존재하면 이미 처리된 이벤트로 간주 가능). */
    public boolean isProcessed(String eventId, String consumerType) {
        Boolean exists = stringRedisTemplate.hasKey(buildKey(eventId, consumerType));
        return Boolean.TRUE.equals(exists);
    }

    /**
     * 처리 완료 표시. TTL은 설정의 시간 단위로 두어 장기적으로 키가 쌓이지 않게 한다.
     * DB 반영 이후에 호출해야 한다(커밋 후 등).
     */
    public void markProcessed(String eventId, String consumerType) {
        stringRedisTemplate.opsForValue().set(
                buildKey(eventId, consumerType),
                VALUE_MARKER,
                Duration.ofHours(ttlHours)
        );
    }

    /** 수동 재처리 전에 idempotency 캐시를 비운다. */
    public void clearProcessed(String eventId, String consumerType) {
        stringRedisTemplate.delete(buildKey(eventId, consumerType));
    }
}
