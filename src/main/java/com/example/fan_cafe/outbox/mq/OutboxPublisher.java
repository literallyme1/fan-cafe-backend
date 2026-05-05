package com.example.fan_cafe.outbox.mq;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpTimeoutException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_EXCHANGE;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_ROUTING_KEY;

@Slf4j
@Component
public class OutboxPublisher implements com.example.fan_cafe.outbox.application.OutboxPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final long publisherConfirmTimeoutMs;

    public OutboxPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${outbox.publisher.confirm-timeout-ms:5000}") long publisherConfirmTimeoutMs
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.publisherConfirmTimeoutMs = publisherConfirmTimeoutMs;
    }

    @Override
    public void publish(String payload, String traceId) {

        boolean confirmed = Boolean.TRUE.equals(rabbitTemplate.invoke(ops -> {
            ops.convertAndSend(OUTBOX_EXCHANGE, OUTBOX_ROUTING_KEY, payload, message -> {
                String tid = traceId != null ? traceId : MDC.get("traceId");
                message.getMessageProperties().setHeader("traceId", tid);
                return message;
            });
            return ops.waitForConfirms(publisherConfirmTimeoutMs);
        }));

        if (!confirmed) {
            throw new AmqpTimeoutException(
                    "Publisher confirm timed out after " + publisherConfirmTimeoutMs + " ms (no broker ACK)"
            );
        }
    }
}
