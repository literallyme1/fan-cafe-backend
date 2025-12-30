package com.example.fan_cafe.notification.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.fan_cafe.notification.infrastructure.messaging.NotificationMqNames.*;

@Configuration
public class NotificationRabbitConfig {

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(
                MAIN_EXCHANGE
        );
    }

    //실패한 메세지가 모임.
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DLX);
    }


    //넣을 큐 생성, DLQ 규칙도 부착
    @Bean
    public Queue notificationQueue() {
        //durable, 서버 재시작 시 유지
        return QueueBuilder.durable(MAIN_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    //버려진 메세지가 쌓이는 큐
    @Bean
    public Queue notificationDlq() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    //Queue, Exchange, 라우팅 규칙 를 연결
    @Bean
    public Binding noficationBinding(
            @Qualifier("notificationQueue") Queue notificationQueue,
            @Qualifier("notificationExchange") TopicExchange mainExchange
    ) {
        return BindingBuilder
                .bind(notificationQueue)
                .to(mainExchange)
                .with(MAIN_ROUTING_KEY);
    }

    //DLX → DLQ 연결
    @Bean
    public Binding dlqBinding(
            @Qualifier("notificationDlq") Queue notificationDlq,
            @Qualifier("deadLetterExchange") TopicExchange deadLetterExchange
    ) {
        return BindingBuilder
                .bind(notificationDlq)
                .to(deadLetterExchange)
                .with(DLQ_ROUTING_KEY);
    }


}
