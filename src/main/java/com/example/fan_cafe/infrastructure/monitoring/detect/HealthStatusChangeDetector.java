package com.example.fan_cafe.infrastructure.monitoring.detect;


import com.example.fan_cafe.notification.trigger.HealthCheckNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

//상태 변화 감지
@Component
@RequiredArgsConstructor
public class HealthStatusChangeDetector {

    private final HealthStatusRegistry registry;
    private final HealthCheckNotifier notifier;

    public void detect(String component, ComponentHealthStatus current) {
        ComponentHealthStatus previous = registry.get(component);

        if (previous == null) {
            registry.update(component, current);
        }
        //이전과, 현재 상태가 다를 시
        if (previous != current) {
            notifier.notifyStatusChange(component, previous, current);
            registry.update(component, current);
        }
    }
}
