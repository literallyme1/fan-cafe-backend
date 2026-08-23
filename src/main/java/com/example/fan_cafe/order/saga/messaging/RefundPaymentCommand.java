package com.example.fan_cafe.order.saga.messaging;

import java.util.UUID;

public record RefundPaymentCommand(
        String eventType,
        UUID sagaId,
        Long orderId,
        String reason,
        String idempotencyKey
) {
    public static final String EVENT_TYPE = "REFUND_PAYMENT";

    public static RefundPaymentCommand of(UUID sagaId, Long orderId, String reason) {
        return new RefundPaymentCommand(
                EVENT_TYPE,
                sagaId,
                orderId,
                reason,
                "REFUND:" + sagaId
        );
    }
}
