package com.example.fan_cafe.order.payment.client;

import java.time.LocalDateTime;

public record PaymentResultResponse(
        Long orderId,
        PaymentResultStatus status,
        String paymentKey,
        String failureReason,
        String failureCode,
        LocalDateTime approvedAt
) {
    public PaymentResultResponse(
            Long orderId,
            PaymentResultStatus status,
            String paymentKey,
            String failureReason,
            String failureCode
    ) {
        this(orderId, status, paymentKey, failureReason, failureCode, null);
    }
}
