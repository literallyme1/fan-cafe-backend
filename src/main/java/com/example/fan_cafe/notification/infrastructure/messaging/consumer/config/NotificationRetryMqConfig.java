package com.example.fan_cafe.notification.infrastructure.messaging.consumer.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.fan_cafe.notification.infrastructure.messaging.NotificationMqNames.*;

@Configuration
public class NotificationRetryMqConfig {

    //Retry Exchange
    @Bean
    public DirectExchange notificationRetryExchange() {
        return new DirectExchange(RETRY_EXCHANGE, true, false);
    }

    // 5초 쉬었다가 다시 메인 큐로 돌려보내는 대기소
    @Bean
    public Queue notificationRetry5sQueue() {
        return QueueBuilder.durable(RETRY_5S_QUEUE)
                .ttl(RETRY_5S_TTL_MS) //5초 대기
                .deadLetterExchange(MAIN_EXCHANGE) //5초 지나면 메인으로 감.
                .deadLetterRoutingKey(MAIN_ROUTING_KEY) //5초짜리 retry 큐로 가야 한다 라고 표시
                .build();
    }

    //30초 쉬었다가 다시 메인 큐로 돌려보내는 대기소
    @Bean
    public Queue notification30sQueue() {
        return QueueBuilder.durable(RETRY_30S_QUEUE)
                .ttl(RETRY_30S_TTL_MS)
                .deadLetterExchange(MAIN_EXCHANGE)
                .deadLetterRoutingKey(MAIN_ROUTING_KEY)
                .build();
    }

    //retry exchange 에 온 메세지 5초 대기 큐에 연결
    @Bean
    public Binding bindRetry5s(DirectExchange notificationRetryExchange,
                               Queue notificationRetry5sQueue) {
        return BindingBuilder.bind(notificationRetry5sQueue)
                .to(notificationRetryExchange)
                .with(RETRY_5S_ROUTING_KEY);
    }

    @Bean
    public Binding bindRetry30s(DirectExchange notificationRetryExchange,
                               Queue notificationRetry30sQueue) {
        return BindingBuilder.bind(notificationRetry30sQueue)
                .to(notificationRetryExchange)
                .with(RETRY_30S_ROUTING_KEY);
    }

}
