package com.example.fan_cafe.notification.trigger;


import com.example.fan_cafe.notification.domain.NotificationEvent;
import com.example.fan_cafe.notification.domain.NotificationLevel;
import com.example.fan_cafe.notification.domain.NotificationOpsType;
import com.example.fan_cafe.notification.guard.NotificationRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

//redis 운영 중 실패 알림
@Component
@RequiredArgsConstructor
public class RedisErrorNotifier {

    private final ApplicationEventPublisher publisher;
    private final NotificationRateLimiter rateLimiter;

    public void notify(Exception e) {
        String key = "REDIS:ERROR";

        if (!rateLimiter.allow(key)) {
            return;
        }

        publisher.publishEvent(
                NotificationEvent.of(
                        NotificationOpsType.REDIS,
                        NotificationLevel.ERROR,
                        "Redis error occurred",
                        e.getMessage(),
                        null
                )
        );
    }
}
