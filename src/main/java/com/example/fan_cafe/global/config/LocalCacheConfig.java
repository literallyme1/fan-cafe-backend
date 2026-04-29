package com.example.fan_cafe.global.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Configuration
public class LocalCacheConfig {

    // 기본 TTL(만료 시간) = 30초
    private static final long BASE_EXPIRE_SECONDS = 30L;

    // jitter 비율 = 20%
    // 실제 만료 시간은 24초~36초 사이에서 랜덤으로 결정된다.
    private static final double JITTER_RATIO = 0.2d;

    // Caffeine Expiry는 나노초 단위를 사용하므로 미리 변환해 둔다.
    private static final long BASE_EXPIRE_NANOS = TimeUnit.SECONDS.toNanos(BASE_EXPIRE_SECONDS);

    @Bean(name = "commentCountLocalCache")
    public Cache<String, Integer> commentCountLocalCache() {
        return Caffeine.newBuilder()
                // 프로세스(인스턴스) 1개 기준 최대 10,000개 키를 보관
                .maximumSize(10_000)
                // 엔트리마다 서로 다른 만료 시간을 주기 위해 커스텀 Expiry 사용
                .expireAfter(new Expiry<String, Integer>() {
                    @Override
                    public long expireAfterCreate(String key, Integer value, long currentTime) {
                        // 캐시 최초 저장 시: 30초 ±20% 랜덤 TTL 적용
                        return ttlWithJitterNanos();
                    }

                    @Override
                    public long expireAfterUpdate(String key, Integer value, long currentTime, long currentDuration) {
                        // 값 갱신 시: 다시 30초 ±20% 랜덤 TTL로 재설정
                        return ttlWithJitterNanos();
                    }

                    @Override
                    public long expireAfterRead(String key, Integer value, long currentTime, long currentDuration) {
                        // 조회(read)만으로 TTL을 늘리지 않는다.
                        return currentDuration;
                    }
                })
                .build();
    }

    private long ttlWithJitterNanos() {
        // 예) BASE=30초, JITTER=20% -> factor는 0.8~1.2
        // 최종 TTL = BASE * factor
        double min = 1.0d - JITTER_RATIO;
        double max = 1.0d + JITTER_RATIO;
        double factor = ThreadLocalRandom.current().nextDouble(min, max);
        return (long) (BASE_EXPIRE_NANOS * factor);
    }
}
