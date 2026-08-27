package com.example.fan_cafe.order.saga.messaging;

import java.math.BigDecimal;

public record ApprovePaymentCommand(
        String eventType,
        Long orderId,
        BigDecimal expectedAmount,
        BigDecimal approvalAmount,
        String paymentKey
) {
    public static final String EVENT_TYPE = "APPROVE_PAYMENT";

    public static ApprovePaymentCommand of(
            Long orderId,
            BigDecimal expectedAmount,
            BigDecimal approvalAmount,
            String paymentKey
    ) {
        return new ApprovePaymentCommand(
                EVENT_TYPE, orderId, expectedAmount, approvalAmount, paymentKey);
    }
}
