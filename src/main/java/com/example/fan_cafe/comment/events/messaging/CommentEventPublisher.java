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
    //TODO : EXCHANGE 공용화로 변경
    //Producer 가 이벤트를 MQ에 던짐.
//    public void publish(CommentCreatedEvent event) {
//        rabbitTemplate.convertAndSend(
//                CommentRabbitConstants.COMMENT_EXCHANGE,  //exchange
//                CommentRabbitConstants.COMMENT_ROUTING_KEY,  //routing key
//                event
//        );
//    }

    public void publish(CommentCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                MAIN_EXCHANGE,  //exchange
                MAIN_ROUTING_KEY,  //routing key
                event
        );
    }
}
