package com.example.payment.messaging;

import org.springframework.amqp.AmqpTimeoutException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.example.payment.messaging.PaymentRefundMqNames.EXCHANGE;
import static com.example.payment.messaging.PaymentRefundMqNames.RESULT_ROUTING_KEY;

@Component
public class PaymentRefundResultPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final long confirmTimeoutMs;

    public PaymentRefundResultPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${payment.refund-result.publisher-confirm-timeout-ms:5000}") long confirmTimeoutMs
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.confirmTimeoutMs = confirmTimeoutMs;
    }

    public void publish(PaymentRefundedResult result) {
        boolean confirmed = Boolean.TRUE.equals(rabbitTemplate.invoke(operations -> {
            operations.convertAndSend(EXCHANGE, RESULT_ROUTING_KEY, result);
            return operations.waitForConfirms(confirmTimeoutMs);
        }));
        if (!confirmed) {
            throw new AmqpTimeoutException("Payment refund result publisher confirm timed out");
        }
    }
}
