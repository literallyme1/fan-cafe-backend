package com.example.fan_cafe.comment.events;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publish(CommentCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                "comment.exchange",  //exchange
                "comment.created",  //routing key
                event
        );
    }
}
