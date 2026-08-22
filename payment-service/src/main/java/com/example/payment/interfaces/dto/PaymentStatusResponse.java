package com.example.payment.interfaces.dto;

import com.example.payment.domain.Payment;
import com.example.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentStatusResponse(
        Long orderId,
        PaymentStatus status,
        BigDecimal expectedAmount,
        BigDecimal approvedAmount,
        String paymentKey,
        String failureReason,
        String refundIdempotencyKey,
        String refundReason,
        LocalDateTime refundedAt
) {
    public static PaymentStatusResponse from(Payment payment) {
        return new PaymentStatusResponse(
                payment.getOrderId(),
                payment.getStatus(),
                payment.getExpectedAmount(),
                payment.getApprovedAmount(),
                payment.getPaymentKey(),
                payment.getFailureReason(),
                payment.getRefundIdempotencyKey(),
                payment.getRefundReason(),
                payment.getRefundedAt()
        );
    }
}
