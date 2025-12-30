package com.example.fan_cafe.notification.infrastructure.messaging.retry;

import com.example.fan_cafe.notification.application.retry.RetryTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.example.fan_cafe.notification.infrastructure.messaging.NotificationMqNames.*;

@Component
@RequiredArgsConstructor
public class RetryRouter {

    private final RabbitTemplate rabbitTemplate;

    //결정 대상을 큐로 보낸다.
    public void routeRetry(Message message, RetryTarget target) {
        switch (target) {
            case RETRY_5S ->
                rabbitTemplate.send(RETRY_EXCHANGE, RETRY_5S_ROUTING_KEY, message); //5초 큐

            case RETRY_30S ->
                rabbitTemplate.send(RETRY_EXCHANGE, RETRY_30S_ROUTING_KEY, message); //30초 큐

            case DLQ ->
                rabbitTemplate.send(DLQ_EXCHANGE,  DLQ_ROUTING_KEY, message);
        }
    }
}
