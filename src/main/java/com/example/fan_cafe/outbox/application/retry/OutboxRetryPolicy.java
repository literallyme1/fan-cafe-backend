package com.example.fan_cafe.outbox.application.retry;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class OutboxRetryPolicy {

    private static final long BASE_DELAY_SECONDS = 5L;
    private static final long MAX_DELAY_SECONDS = 300L;
    private static final double JITTER_MIN = 0.8;
    private static final double JITTER_MAX = 1.2;

    public LocalDateTime nextRetryWithExponentialBackoffAndJitter(int retryCount) {
        long exponentialDelay = BASE_DELAY_SECONDS * (1L << Math.max(0, retryCount));
        long cappedDelay = Math.min(exponentialDelay, MAX_DELAY_SECONDS);
        double jitterFactor = ThreadLocalRandom.current().nextDouble(JITTER_MIN, JITTER_MAX);
        long finalDelay = Math.max(1L, Math.round(cappedDelay * jitterFactor));
        return LocalDateTime.now().plusSeconds(finalDelay);
    }
}
