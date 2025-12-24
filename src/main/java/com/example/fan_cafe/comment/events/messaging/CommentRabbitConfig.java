package com.example.fan_cafe.comment.events.messaging;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.fan_cafe.comment.events.messaging.CommentRabbitConstants.*;

@Configuration
public class CommentRabbitConfig {

    @Bean
    public TopicExchange commentExchange() {
        return new TopicExchange(
                COMMENT_EXCHANGE
        );
    }

    //실패한 메세지가 모임.
    @Bean
    public TopicExchange commentDeadLetterExchange() {
        return new TopicExchange(COMMENT_DLX);
    }


    //넣을 큐 생성, DLQ 규칙도 부착
    @Bean
    public Queue commentNotificationQueue() {
        //durable, 서버 재시작 시 유지
        return QueueBuilder.durable(COMMENT_QUEUE)
                .withArgument("x-dead-letter-exchange", COMMENT_DLX)
                .withArgument("x-dead-letter-routing-key", COMMENT_DLQ_ROUTING_KEY)
                .build();
    }

    //버려진 메세지가 쌓이는 큐
    @Bean
    public Queue commentNotificationDlq() {
        return QueueBuilder.durable(COMMENT_DLQ).build();
    }

    //Queue, Exchange, 라우팅 규칙 를 연결
    @Bean
    public Binding commentNoficationBinding(
            Queue commentNotificationQueue,
            TopicExchange commentExchange
    ) {
        return BindingBuilder
                .bind(commentNotificationQueue)
                .to(commentExchange)
                .with(COMMENT_ROUTING_KEY);
    }

    //DLX → DLQ 연결
    @Bean
    public Binding commentDlqBinding(
            Queue commentNotificationDlq,
            TopicExchange commentDeadLetterExchange
    ) {
        return BindingBuilder
                .bind(commentNotificationDlq)
                .to(commentDeadLetterExchange)
                .with(COMMENT_DLQ_ROUTING_KEY);
    }


}
