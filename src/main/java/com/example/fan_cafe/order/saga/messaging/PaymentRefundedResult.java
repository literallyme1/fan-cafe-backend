package com.example.fan_cafe.order.saga.messaging;

import com.example.fan_cafe.order.payment.client.PaymentResultStatus;

import java.util.UUID;

public record PaymentRefundedResult(
        String eventType,
        UUID sagaId,
        Long orderId,
        PaymentResultStatus status,
        String refundIdempotencyKey
) {
    public static final String EVENT_TYPE = "PAYMENT_REFUNDED";
}
