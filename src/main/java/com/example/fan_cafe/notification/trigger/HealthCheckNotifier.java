package com.example.fan_cafe.notification.trigger;

import com.example.fan_cafe.infrastructure.monitoring.detect.ComponentHealthStatus;
import com.example.fan_cafe.notification.domain.NotificationEvent;
import com.example.fan_cafe.notification.domain.NotificationLevel;
import com.example.fan_cafe.notification.domain.NotificationOpsType;
import com.example.fan_cafe.notification.guard.NotificationRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

//Health 상태 이상 -> 알림 이벤트 발생
@Component
@RequiredArgsConstructor
public class HealthCheckNotifier {

    private final ApplicationEventPublisher publisher;
    private final NotificationRateLimiter rateLimiter;

    //health 가 다운 됐을 때 호출 -> 알림 이벤트 생성 발행
    public void notifyDown(String componentName, String detail) {

        String key = "HEALTH:" + componentName;

        if (!rateLimiter.allow(key)) {
            return;
        }

        publisher.publishEvent(
                NotificationEvent.of(
                        NotificationOpsType.HEALTH,
                        NotificationLevel.ERROR,
                        componentName + "health check failed",
                        detail,
                        null
                )
        );
    }

    public void notifyStatusChange(
            String component,
            ComponentHealthStatus prev,
            ComponentHealthStatus current
    ){
        publisher.publishEvent(
                NotificationEvent.of(
                        NotificationOpsType.HEALTH,
                        current == ComponentHealthStatus.DOWN
                        ? NotificationLevel.ERROR
                                :NotificationLevel.INFO,
                        component + "health changed", //알림 제목
                        prev + " → " + current, //알림 설명
                        null
                )
        );
    }
}
