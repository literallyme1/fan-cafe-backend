package com.example.fan_cafe.outbox;

import com.example.fan_cafe.outbox.application.retry.OutboxRetryPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxRetryPolicyTest {

    private final OutboxRetryPolicy retryPolicy = new OutboxRetryPolicy();

    @Test
    @DisplayName("retryCount=0이면 4~6초 범위의 nextRetry가 계산된다.")
    void nextRetry_shouldApplyBaseDelayWithJitter() {
        LocalDateTime before = LocalDateTime.now();
        LocalDateTime nextRetryAt = retryPolicy.nextRetryWithExponentialBackoffAndJitter(0);
        LocalDateTime after = LocalDateTime.now();

        long minSeconds = 4L;
        long maxSeconds = 6L;

        assertThat(nextRetryAt).isAfterOrEqualTo(before.plusSeconds(minSeconds));
        assertThat(nextRetryAt).isBeforeOrEqualTo(after.plusSeconds(maxSeconds));
    }

    @Test
    @DisplayName("지수 백오프는 최대 5분 상한을 넘지 않는다.")
    void nextRetry_shouldCapDelayAtFiveMinutes() {
        LocalDateTime before = LocalDateTime.now();
        LocalDateTime nextRetryAt = retryPolicy.nextRetryWithExponentialBackoffAndJitter(10);
        LocalDateTime after = LocalDateTime.now();

        long minSeconds = 240L; // 300 * 0.8
        long maxSeconds = 360L; // 300 * 1.2

        assertThat(nextRetryAt).isAfterOrEqualTo(before.plusSeconds(minSeconds));
        assertThat(nextRetryAt).isBeforeOrEqualTo(after.plusSeconds(maxSeconds));
    }
}
