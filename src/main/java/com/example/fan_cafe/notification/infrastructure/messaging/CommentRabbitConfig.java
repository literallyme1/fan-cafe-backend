package com.example.fan_cafe.notification.infrastructure.messaging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

@Configuration
public class CommentRabbitConfig {

    @Bean
    public TopicExchange commentExchange() {
        return new TopicExchange(
                CommentRabbitConstants.COMMENT_EXCHANGE
        );
    }

    @Bean
    public Queue commentNotificationQueue() {
        return new Queue(
            CommentRabbitConstants.COMMENT_QUEUE,
            true //durable, MQ 재시작 시 Queue 유지
        );
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
                .with(CommentRabbitConstants.COMMENT_ROUTING_KEY);
    }

}
