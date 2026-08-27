package com.example.payment.messaging;

import java.math.BigDecimal;

public record ApprovePaymentCommand(
        String eventType,
        Long orderId,
        BigDecimal expectedAmount,
        BigDecimal approvalAmount,
        String paymentKey
) {
    public static final String EVENT_TYPE = "APPROVE_PAYMENT";
}
