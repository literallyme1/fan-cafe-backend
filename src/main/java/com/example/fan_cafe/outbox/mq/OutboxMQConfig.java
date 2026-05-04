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

/**
 * Outbox 메인 큐·단계별 Retry(TTL 백오프)·DLQ 및 동일 {@link OutboxMQNames#OUTBOX_EXCHANGE} 바인딩.
 *
 * <p>retry 큐는 “처리 대기함”으로만 쓰이며 별도 리스너가 없다. 브로커가 설정한 TTL만큼 메시지를 보관한 뒤
 * 만료 시점에 dead-letter 교환으로 원본 흐름({@link OutboxMQNames#OUTBOX_EXCHANGE} +
 * {@link OutboxMQNames#OUTBOX_ROUTING_KEY})에 다시 넣어 지연(backoff) 후 재시도하게 한다.
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

    /**
     * 첫 재시도 전 5초 대기. TTL 만료 후 DLX로 {@code outbox.event} 라우팅 키를 써 메인 큐로 복귀한다.
     */
    @Bean
    public Queue outboxRetry5sQueue() {
        return QueueBuilder.durable(OUTBOX_RETRY_5S_QUEUE)
                .withArgument("x-message-ttl", 5_000)
                .withArgument("x-dead-letter-exchange", OUTBOX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", OUTBOX_ROUTING_KEY)
                .build();
    }

    /**
     * 두 번째 재시도 전 30초 대기. 원리는 {@link #outboxRetry5sQueue()}와 동일한 TTL·dead-letter 패턴이다.
     */
    @Bean
    public Queue outboxRetry30sQueue() {
        return QueueBuilder.durable(OUTBOX_RETRY_30S_QUEUE)
                .withArgument("x-message-ttl", 30_000)
                .withArgument("x-dead-letter-exchange", OUTBOX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", OUTBOX_ROUTING_KEY)
                .build();
    }

    /**
     * 세 번째 재시도 전 1분 대기. 단계가 길수록 외부 의존성 복구 시간을 벌기 위한 마지막 지연 단계다.
     */
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
