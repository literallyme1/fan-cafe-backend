package com.example.fan_cafe.order.payment.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentStatusResponse(
        Long orderId,
        PaymentResultStatus status,
        BigDecimal expectedAmount,
        BigDecimal approvedAmount,
        String paymentKey,
        String failureReason,
        String refundIdempotencyKey,
        String refundReason,
        LocalDateTime refundedAt
) {
}
