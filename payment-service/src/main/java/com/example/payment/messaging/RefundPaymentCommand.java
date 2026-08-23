package com.example.payment.messaging;

import java.util.UUID;

public record RefundPaymentCommand(
        String eventType,
        UUID sagaId,
        Long orderId,
        String reason,
        String idempotencyKey,
        String eventId
) {
    public static final String EVENT_TYPE = "REFUND_PAYMENT";

    public RefundPaymentCommand(
            String eventType,
            UUID sagaId,
            Long orderId,
            String reason,
            String idempotencyKey
    ) {
        this(eventType, sagaId, orderId, reason, idempotencyKey, null);
    }
}
