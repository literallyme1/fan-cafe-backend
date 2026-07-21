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
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_RETRY_1M_QUEUE;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_RETRY_1M_ROUTING_KEY;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_RETRY_30S_QUEUE;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_RETRY_30S_ROUTING_KEY;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_RETRY_5S_QUEUE;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_RETRY_5S_ROUTING_KEY;
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

    @Bean
    public Queue outboxRetry5sQueue() {
        return QueueBuilder.durable(OUTBOX_RETRY_5S_QUEUE)
                .withArgument("x-message-ttl", 5_000)
                .withArgument("x-dead-letter-exchange", OUTBOX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", OUTBOX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue outboxRetry30sQueue() {
        return QueueBuilder.durable(OUTBOX_RETRY_30S_QUEUE)
                .withArgument("x-message-ttl", 30_000)
                .withArgument("x-dead-letter-exchange", OUTBOX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", OUTBOX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue outboxRetry1mQueue() {
        return QueueBuilder.durable(OUTBOX_RETRY_1M_QUEUE)
                .withArgument("x-message-ttl", 60_000)
                .withArgument("x-dead-letter-exchange", OUTBOX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", OUTBOX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding outboxRetry5sBinding(
            @Qualifier("outboxRetry5sQueue") Queue outboxRetry5sQueue,
            @Qualifier("outboxExchange") DirectExchange outboxExchange
    ) {
        return BindingBuilder.bind(outboxRetry5sQueue).to(outboxExchange).with(OUTBOX_RETRY_5S_ROUTING_KEY);
    }

    @Bean
    public Binding outboxRetry30sBinding(
            @Qualifier("outboxRetry30sQueue") Queue outboxRetry30sQueue,
            @Qualifier("outboxExchange") DirectExchange outboxExchange
    ) {
        return BindingBuilder.bind(outboxRetry30sQueue).to(outboxExchange).with(OUTBOX_RETRY_30S_ROUTING_KEY);
    }

    @Bean
    public Binding outboxRetry1mBinding(
            @Qualifier("outboxRetry1mQueue") Queue outboxRetry1mQueue,
            @Qualifier("outboxExchange") DirectExchange outboxExchange
    ) {
        return BindingBuilder.bind(outboxRetry1mQueue).to(outboxExchange).with(OUTBOX_RETRY_1M_ROUTING_KEY);
    }

    @Bean
    public Queue outboxDlqQueue() {
        return QueueBuilder.durable(OUTBOX_DLQ_QUEUE).build();
    }

    @Bean
    public Binding outboxDlqBinding(
            @Qualifier("outboxDlqQueue") Queue outboxDlqQueue,
            @Qualifier("outboxExchange") DirectExchange outboxExchange
    ) {
        return BindingBuilder.bind(outboxDlqQueue).to(outboxExchange).with(OUTBOX_DLQ_ROUTING_KEY);
    }
}
