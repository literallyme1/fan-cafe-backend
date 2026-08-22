package com.example.fan_cafe.order.payment.client;

public record PaymentResultResponse(
        Long orderId,
        PaymentResultStatus status,
        String paymentKey,
        String failureReason,
        String failureCode
) {
}
