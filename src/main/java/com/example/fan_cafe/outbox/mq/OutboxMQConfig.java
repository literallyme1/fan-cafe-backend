package com.example.fan_cafe.outbox.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_EXCHANGE;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_QUEUE;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_ROUTING_KEY;

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
}
