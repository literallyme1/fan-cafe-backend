package com.example.fan_cafe.outbox.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_DLQ_QUEUE;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_DLQ_ROUTING_KEY;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_EXCHANGE;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_QUEUE;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_RETRY_QUEUE;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_RETRY_ROUTING_KEY;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_ROUTING_KEY;

/**
 * Outbox 메인 큐·Retry·DLQ 및 동일 {@link OutboxMQNames#OUTBOX_EXCHANGE}에 대한 바인딩.
 */
@Configuration
public class OutboxMQConfig {

    @Bean
    public DirectExchange outboxExchange() {
        return new DirectExchange(OUTBOX_EXCHANGE);
    }

    @Bean
    public Queue outboxQueue() {
        return QueueBuilder.durable(OUTBOX_QUEUE).build();
    }

    @Bean
    public Binding outboxBinding(
            @Qualifier("outboxQueue") Queue outboxQueue,
            @Qualifier("outboxExchange") DirectExchange outboxExchange
    ) {
        return BindingBuilder
                .bind(outboxQueue)
                .to(outboxExchange)
                .with(OUTBOX_ROUTING_KEY);
    }

    @Bean
    public Queue outboxRetryQueue() {
        return QueueBuilder.durable(OUTBOX_RETRY_QUEUE).build(); // 일시 장애 재처리 대기
    }

    @Bean
    public Queue outboxDlqQueue() {
        return QueueBuilder.durable(OUTBOX_DLQ_QUEUE).build();
    }

    @Bean
    public Binding outboxRetryBinding(
            @Qualifier("outboxRetryQueue") Queue outboxRetryQueue,
            @Qualifier("outboxExchange") DirectExchange outboxExchange
    ) {
        return BindingBuilder.bind(outboxRetryQueue).to(outboxExchange).with(OUTBOX_RETRY_ROUTING_KEY);
    }

    @Bean
    public Binding outboxDlqBinding(
            @Qualifier("outboxDlqQueue") Queue outboxDlqQueue,
            @Qualifier("outboxExchange") DirectExchange outboxExchange
    ) {
        return BindingBuilder.bind(outboxDlqQueue).to(outboxExchange).with(OUTBOX_DLQ_ROUTING_KEY);
    }
}
