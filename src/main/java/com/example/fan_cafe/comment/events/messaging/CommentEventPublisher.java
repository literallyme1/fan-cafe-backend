package com.example.fan_cafe.comment.events.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    //Producer 가 이벤트를 MQ에 던짐.
    public void publish(CommentCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                CommentRabbitConstants.COMMENT_EXCHANGE,  //exchange
                CommentRabbitConstants.COMMENT_ROUTING_KEY,  //routing key
                event
        );
    }
}
