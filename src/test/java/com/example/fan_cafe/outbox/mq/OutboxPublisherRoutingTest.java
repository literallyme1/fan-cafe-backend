package com.example.fan_cafe.outbox.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

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
    }
}
