package com.example.payment.interfaces.dto;

import com.example.payment.domain.Payment;
import com.example.payment.domain.PaymentStatus;

public record PaymentResultResponse(
        Long orderId,
        PaymentStatus status,
        String paymentKey,
        String failureReason,
        String failureCode
) {
    public static PaymentResultResponse from(Payment payment) {
        return new PaymentResultResponse(payment.getOrderId(), payment.getStatus(),
                payment.getPaymentKey(), payment.getFailureReason(), null);
    }

    public static PaymentResultResponse amountMismatch(Payment payment) {
        return new PaymentResultResponse(payment.getOrderId(), payment.getStatus(),
                payment.getPaymentKey(), payment.getFailureReason(), "PAYMENT_AMOUNT_MISMATCH");
    }
}
