package com.example.fan_cafe.outbox.mq;

import com.example.fan_cafe.order.saga.messaging.RefundPaymentCommand;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpTimeoutException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_EXCHANGE;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_ROUTING_KEY;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.PAYMENT_REFUND_COMMAND_ROUTING_KEY;

@Slf4j
@Component
public class OutboxPublisher implements com.example.fan_cafe.outbox.application.OutboxPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final long publisherConfirmTimeoutMs;

    public OutboxPublisher(
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            @Value("${outbox.publisher.confirm-timeout-ms:5000}") long publisherConfirmTimeoutMs
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.publisherConfirmTimeoutMs = publisherConfirmTimeoutMs;
    }

    @Override
    public void publish(String payload, String traceId) {
        String routingKey = resolveRoutingKey(payload);

        boolean confirmed = Boolean.TRUE.equals(rabbitTemplate.invoke(ops -> {
            ops.convertAndSend(OUTBOX_EXCHANGE, routingKey, payload, message -> {
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

    String resolveRoutingKey(String payload) {
        try {
            String eventType = objectMapper.readTree(payload).path("eventType").asText();
            return RefundPaymentCommand.EVENT_TYPE.equals(eventType)
                    ? PAYMENT_REFUND_COMMAND_ROUTING_KEY
                    : OUTBOX_ROUTING_KEY;
        } catch (JsonProcessingException invalidPayload) {
            throw new org.springframework.amqp.support.converter.MessageConversionException(
                    "Invalid outbox payload", invalidPayload);
        }
    }
}
