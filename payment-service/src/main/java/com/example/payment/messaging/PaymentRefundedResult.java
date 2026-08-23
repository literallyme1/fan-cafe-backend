package com.example.payment.messaging;

import com.example.payment.domain.PaymentStatus;

import java.util.UUID;

public record PaymentRefundedResult(
        String eventType,
        UUID sagaId,
        Long orderId,
        PaymentStatus status,
        String refundIdempotencyKey
) {
    public static final String EVENT_TYPE = "PAYMENT_REFUNDED";
}
