package com.example.payment.messaging;

import java.util.UUID;

public record RefundPaymentCommand(
        String eventType,
        UUID sagaId,
        Long orderId,
        String reason,
        String idempotencyKey
) {
    public static final String EVENT_TYPE = "REFUND_PAYMENT";
}
