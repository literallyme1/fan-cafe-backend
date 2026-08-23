package com.example.fan_cafe.order.saga.recovery;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class SagaRecoveryBackoffPolicy {
    private final SagaRecoveryProperties properties;
    private final Clock clock;

    public LocalDateTime nextRetryAt(int failureCount) {
        int exponent = Math.min(30, Math.max(0, failureCount - 1));
        long baseMillis = properties.getBaseDelay().toMillis();
        long cappedMillis = Math.min(
                multiplyWithCap(baseMillis, 1L << exponent),
                properties.getMaxDelay().toMillis());
        double jitter = properties.getJitterRatio();
        double factor = jitter == 0.0
                ? 1.0
                : ThreadLocalRandom.current().nextDouble(1.0 - jitter, 1.0 + jitter);
        long delayMillis = Math.max(1L, Math.round(cappedMillis * factor));
        return LocalDateTime.now(clock).plus(Duration.ofMillis(delayMillis));
    }

    private long multiplyWithCap(long value, long multiplier) {
        if (value > Long.MAX_VALUE / multiplier) {
            return Long.MAX_VALUE;
        }
        return value * multiplier;
    }
}
