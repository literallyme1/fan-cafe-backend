package com.example.fan_cafe.outbox.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_ROUTING_KEY;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.PAYMENT_REFUND_COMMAND_ROUTING_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OutboxPublisherRoutingTest {

    private final OutboxPublisher publisher = new OutboxPublisher(
            mock(RabbitTemplate.class), new ObjectMapper(), 1000L);

    @Test
    void refundPaymentCommandUsesDedicatedPaymentRoutingKey() {
        String payload = "{\"eventType\":\"REFUND_PAYMENT\",\"orderId\":10}";

        assertThat(publisher.resolveRoutingKey(payload))
                .isEqualTo(PAYMENT_REFUND_COMMAND_ROUTING_KEY);
    }

    @Test
    void existingEventKeepsExistingOutboxRoutingKey() {
        String payload = "{\"eventType\":\"ORDER_PAID\",\"orderId\":10}";

        assertThat(publisher.resolveRoutingKey(payload)).isEqualTo(OUTBOX_ROUTING_KEY);
        assertThat(publisher.resolveMessagePayload(payload, OUTBOX_ROUTING_KEY)).isEqualTo(payload);
    }

    @Test
    void refundMessageCreatedByOrderPublisherConvertsToPaymentCommand() {
        UUID sagaId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        String payload = """
                {"eventType":"REFUND_PAYMENT","sagaId":"%s","orderId":10,
                 "reason":"order completion failed","idempotencyKey":"REFUND:%s","eventId":"42"}
                """.formatted(sagaId, sagaId);
        Object outboundPayload = publisher.resolveMessagePayload(
                payload, PAYMENT_REFUND_COMMAND_ROUTING_KEY);
        Jackson2JsonMessageConverter orderConverter =
                new Jackson2JsonMessageConverter(new ObjectMapper());
        Jackson2JsonMessageConverter paymentConverter =
                new Jackson2JsonMessageConverter(new ObjectMapper());

        Message message = orderConverter.toMessage(outboundPayload, new MessageProperties());
        publisher.prepareMessage(message, "trace-1", PAYMENT_REFUND_COMMAND_ROUTING_KEY);
        message.getMessageProperties().setInferredArgumentType(
                com.example.payment.messaging.RefundPaymentCommand.class);
        Object converted = paymentConverter.fromMessage(message);

        assertThat(new String(message.getBody(), StandardCharsets.UTF_8)).startsWith("{");
        assertThat(message.getMessageProperties().getContentType())
                .isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
        assertThat(message.getMessageProperties().getHeaders()).doesNotContainKey("__TypeId__");
        assertThat(converted)
                .isInstanceOf(com.example.payment.messaging.RefundPaymentCommand.class);
        com.example.payment.messaging.RefundPaymentCommand command =
                (com.example.payment.messaging.RefundPaymentCommand) converted;
        assertThat(command.sagaId()).isEqualTo(sagaId);
        assertThat(command.orderId()).isEqualTo(10L);
        assertThat(command.idempotencyKey()).isEqualTo("REFUND:" + sagaId);
        assertThat(command.eventId()).isEqualTo("42");
    }
}
