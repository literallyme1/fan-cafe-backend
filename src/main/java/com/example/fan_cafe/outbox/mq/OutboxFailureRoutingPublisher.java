package com.example.fan_cafe.outbox.mq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_DLQ_ROUTING_KEY;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_EXCHANGE;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_RETRY_ROUTING_KEY;

/**
 * 일시 오류·영구 오류 시 원본 페이로드를 각각 Retry 큐 / DLQ로 보낸다.
 * 메인 큐에서는 컨슈머가 먼저 ACK 하므로, 재처리는 이 큐들을 구독하는 쪽에서 정책을 정한다.
 */
@Component
@RequiredArgsConstructor
public class OutboxFailureRoutingPublisher {

    private final RabbitTemplate rabbitTemplate;

    /** 라우팅 키 {@code outbox.retry} → {@link OutboxMQNames#OUTBOX_RETRY_QUEUE} */
    public void publishToRetryQueue(String payload) {
        rabbitTemplate.convertAndSend(OUTBOX_EXCHANGE, OUTBOX_RETRY_ROUTING_KEY, payload);
    }

    /** 라우팅 키 {@code outbox.dlq} → {@link OutboxMQNames#OUTBOX_DLQ_QUEUE} */
    public void publishToDlq(String payload) {
        rabbitTemplate.convertAndSend(OUTBOX_EXCHANGE, OUTBOX_DLQ_ROUTING_KEY, payload);
    }
}
