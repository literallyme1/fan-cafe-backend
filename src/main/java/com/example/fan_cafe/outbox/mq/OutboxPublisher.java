package com.example.fan_cafe.outbox.mq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_EXCHANGE;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_ROUTING_KEY;

@Component
@RequiredArgsConstructor
public class OutboxPublisher implements com.example.fan_cafe.outbox.application.OutboxPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(String payload) {
        rabbitTemplate.convertAndSend(OUTBOX_EXCHANGE, OUTBOX_ROUTING_KEY, payload);
    }
}
