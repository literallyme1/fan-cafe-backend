package com.example.fan_cafe.infrastructure.monitoring.health;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.Lifecycle;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RabbitConsumerHealthIndicator implements HealthIndicator {

    private final RabbitListenerEndpointRegistry registry; //모든 Consumer 목록 가져오기

    @Override
    public Health health() {
        try {
            //하나라도 running 하나?
            boolean anyRunning = registry.getListenerContainers()
                    .stream()
                    .anyMatch(Lifecycle::isRunning);

            if (anyRunning) {
                return Health.up()
                        .withDetail("consumer", "RUNNING")
                        .build();
            }
            //하나라도 처리할 수 있는 consumer 가 X -> down
            //나는 공용화로 사용
            return Health.down()
                    .withDetail("consumer", "ALL_STOPPED")
                    .withDetail("action", "CHECK_CONSUMER_OR_DEPLOY")
                    .build();

        } catch (Exception e) {

            //예외는 절대 던지지 말고 Health로 변환
            return Health.down()
                    .withDetail("reason", "RABBIT_CONSUMER_CHECK_FAILED")
                    .withDetail("error", e.getClass().getSimpleName())
                    .build();

        }
    }
}
