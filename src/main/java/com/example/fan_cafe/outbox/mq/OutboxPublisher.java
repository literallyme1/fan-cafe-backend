package com.example.fan_cafe.outbox.mq;

import com.example.fan_cafe.order.saga.messaging.RefundPaymentCommand;
import com.example.fan_cafe.order.saga.messaging.ApprovePaymentCommand;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.AmqpTimeoutException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.AbstractJavaTypeMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_EXCHANGE;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_ROUTING_KEY;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.PAYMENT_REFUND_COMMAND_ROUTING_KEY;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.PAYMENT_APPROVAL_COMMAND_ROUTING_KEY;

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
        Object messagePayload = resolveMessagePayload(payload, routingKey);

        boolean confirmed = Boolean.TRUE.equals(rabbitTemplate.invoke(ops -> {
            ops.convertAndSend(OUTBOX_EXCHANGE, routingKey, messagePayload,
                    message -> prepareMessage(message, traceId, routingKey));
            return ops.waitForConfirms(publisherConfirmTimeoutMs);
        }));

        if (!confirmed) {
            throw new AmqpTimeoutException(
                    "Publisher confirm timed out after " + publisherConfirmTimeoutMs + " ms (no broker ACK)"
            );
        }
    }

    Message prepareMessage(Message message, String traceId, String routingKey) {
        String tid = traceId != null ? traceId : MDC.get("traceId");
        message.getMessageProperties().setHeader("traceId", tid);
        if (PAYMENT_REFUND_COMMAND_ROUTING_KEY.equals(routingKey)
                || PAYMENT_APPROVAL_COMMAND_ROUTING_KEY.equals(routingKey)) {
            message.getMessageProperties().getHeaders()
                    .remove(AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME);
        }
        return message;
    }

    Object resolveMessagePayload(String payload, String routingKey) {
        if (!PAYMENT_REFUND_COMMAND_ROUTING_KEY.equals(routingKey)
                && !PAYMENT_APPROVAL_COMMAND_ROUTING_KEY.equals(routingKey)) {
            return payload;
        }
        try {
            return PAYMENT_APPROVAL_COMMAND_ROUTING_KEY.equals(routingKey)
                    ? objectMapper.readValue(payload, ApprovePaymentCommand.class)
                    : objectMapper.readValue(payload, RefundPaymentCommand.class);
        } catch (JsonProcessingException invalidPayload) {
            throw messageConversionException(invalidPayload);
        }
    }

    String resolveRoutingKey(String payload) {
        try {
            String eventType = objectMapper.readTree(payload).path("eventType").asText();
            if (RefundPaymentCommand.EVENT_TYPE.equals(eventType)) {
                return PAYMENT_REFUND_COMMAND_ROUTING_KEY;
            }
            return ApprovePaymentCommand.EVENT_TYPE.equals(eventType)
                    ? PAYMENT_APPROVAL_COMMAND_ROUTING_KEY
                    : OUTBOX_ROUTING_KEY;
        } catch (JsonProcessingException invalidPayload) {
            throw messageConversionException(invalidPayload);
        }
    }

    private org.springframework.amqp.support.converter.MessageConversionException messageConversionException(
            JsonProcessingException cause
    ) {
        return new org.springframework.amqp.support.converter.MessageConversionException(
                "Invalid outbox payload", cause);
    }
}
