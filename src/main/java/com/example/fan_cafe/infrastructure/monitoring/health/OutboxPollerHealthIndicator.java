package com.example.fan_cafe.infrastructure.monitoring.health;

import com.example.fan_cafe.outbox.application.OutboxPoller;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OutboxPollerHealthIndicator implements HealthIndicator {

    private static final Duration STUCK_THRESHOLD = Duration.ofMinutes(5);

    private final OutboxPoller outboxPoller;

    @Override
    public Health health() {
        LocalDateTime lastExecutedAt = outboxPoller.getLastExecutedAt();
        if (lastExecutedAt == null) {
            return Health.down()
                    .withDetail("poller", "NOT_STARTED")
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();
        Duration elapsed = Duration.between(lastExecutedAt, now);
        if (elapsed.compareTo(STUCK_THRESHOLD) < 0) {
            return Health.up()
                    .withDetail("poller", "RUNNING")
                    .withDetail("lastExecutedAt", lastExecutedAt)
                    .build();
        }

        return Health.down()
                .withDetail("poller", "POLLER_STUCK")
                .withDetail("lastExecutedAt", lastExecutedAt)
                .withDetail("elapsedSeconds", elapsed.getSeconds())
                .build();
    }
}
