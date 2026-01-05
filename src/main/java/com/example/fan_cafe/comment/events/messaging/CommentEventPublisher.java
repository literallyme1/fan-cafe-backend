package com.example.fan_cafe.comment.events.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.example.fan_cafe.notification.infrastructure.messaging.NotificationMqNames.MAIN_EXCHANGE;
import static com.example.fan_cafe.notification.infrastructure.messaging.NotificationMqNames.MAIN_ROUTING_KEY;

@Component
@RequiredArgsConstructor
public class CommentEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publish(CommentCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                MAIN_EXCHANGE,  //exchange
                MAIN_ROUTING_KEY,  //routing key
                event
        );
    }
}
