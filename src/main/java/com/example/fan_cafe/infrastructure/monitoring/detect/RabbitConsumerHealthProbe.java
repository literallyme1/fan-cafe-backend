package com.example.fan_cafe.infrastructure.monitoring.detect;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.stereotype.Component;
import org.springframework.context.Lifecycle;

@Component
@RequiredArgsConstructor
public class RabbitConsumerHealthProbe {

    private final RabbitListenerEndpointRegistry registry;

    public ComponentHealthStatus check() {
        try {
            boolean anyRunning = registry.getListenerContainers()
                    .stream()
                    .anyMatch(Lifecycle::isRunning);

            return anyRunning
                    ? ComponentHealthStatus.UP
                    : ComponentHealthStatus.DOWN;

        } catch (Exception e) {
            return ComponentHealthStatus.DOWN;
        }
    }
}
