package com.example.payment.interfaces.dto;

import com.example.payment.domain.Payment;
import com.example.payment.domain.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentResultResponse(
        Long orderId,
        PaymentStatus status,
        String paymentKey,
        String failureReason,
        String failureCode,
        LocalDateTime approvedAt
) {
    public PaymentResultResponse(
            Long orderId,
            PaymentStatus status,
            String paymentKey,
            String failureReason,
            String failureCode
    ) {
        this(orderId, status, paymentKey, failureReason, failureCode, null);
    }

    public static PaymentResultResponse from(Payment payment) {
        return new PaymentResultResponse(payment.getOrderId(), payment.getStatus(),
                payment.getPaymentKey(), payment.getFailureReason(), null, payment.getApprovedAt());
    }

    public static PaymentResultResponse amountMismatch(Payment payment) {
        return new PaymentResultResponse(payment.getOrderId(), payment.getStatus(),
                payment.getPaymentKey(), payment.getFailureReason(), "PAYMENT_AMOUNT_MISMATCH",
                payment.getApprovedAt());
    }
}
