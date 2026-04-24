package com.example.fan_cafe.outbox.application.retry;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class RetryPolicy {

    private static final long BASE_DELAY_SECONDS = 5L;
    private static final long MAX_DELAY_SECONDS = 300L; // 5 minutes
    private static final double JITTER_MIN = 0.8; // -20%
    private static final double JITTER_MAX = 1.2; // +20%

    // retryCount 기준 지수 백오프와 jitter를 적용해 다음 재시도 시각을 계산한다.
    public LocalDateTime nextRetry(int retryCount) {
        long exponentialDelay = BASE_DELAY_SECONDS * (1L << Math.max(0, retryCount)); //거듭제곱
        long cappedDelay = Math.min(exponentialDelay, MAX_DELAY_SECONDS); // 상한선
        double jitterFactor = ThreadLocalRandom.current().nextDouble(JITTER_MIN, JITTER_MAX); //jitter +-20%
        long finalDelay = Math.max(1L, Math.round(cappedDelay * jitterFactor)); //최종
        return LocalDateTime.now().plusSeconds(finalDelay);
    }
}
