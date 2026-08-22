package com.example.payment.application;

import com.example.payment.domain.Payment;
import com.example.payment.domain.PaymentStatus;
import com.example.payment.exception.PaymentErrorCode;
import com.example.payment.exception.PaymentException;
import com.example.payment.infrastructure.PaymentRepository;
import com.example.payment.interfaces.dto.PaymentResultResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PaymentService {
    private static final String DEFAULT_FAILURE_REASON = "mock payment failed";
    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public PaymentResultResponse approve(
            Long orderId,
            BigDecimal expectedAmount,
            BigDecimal approvalAmount,
            String paymentKey
    ) {
        validateApproval(expectedAmount, approvalAmount, paymentKey);
        String normalizedKey = paymentKey.trim();
        Payment payment = findOrCreate(orderId, expectedAmount);
        verifyExpectedAmount(payment, expectedAmount);

        if (payment.getStatus() == PaymentStatus.APPROVED) {
            if (payment.isApprovedWith(normalizedKey)) {
                return PaymentResultResponse.from(payment);
            }
            throw new PaymentException(PaymentErrorCode.PAYMENT_ALREADY_APPROVED);
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentException(PaymentErrorCode.INVALID_PAYMENT_STATE);
        }

        if (expectedAmount.compareTo(approvalAmount) != 0) {
            payment.fail("approval amount mismatch");
            return PaymentResultResponse.amountMismatch(payment);
        }

        payment.approve(approvalAmount, normalizedKey);
        return PaymentResultResponse.from(payment);
    }

    @Transactional
    public PaymentResultResponse fail(Long orderId, BigDecimal expectedAmount, String reason) {
        if (expectedAmount == null) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_AMOUNT_REQUIRED);
        }
        Payment payment = findOrCreate(orderId, expectedAmount);
        verifyExpectedAmount(payment, expectedAmount);

        if (payment.getStatus() == PaymentStatus.FAILED) {
            return PaymentResultResponse.from(payment);
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentException(PaymentErrorCode.INVALID_PAYMENT_STATE);
        }

        payment.fail(resolveReason(reason));
        return PaymentResultResponse.from(payment);
    }

    private Payment findOrCreate(Long orderId, BigDecimal expectedAmount) {
        return paymentRepository.findByOrderIdForUpdate(orderId)
                .orElseGet(() -> paymentRepository.save(Payment.pending(orderId, expectedAmount)));
    }

    private void verifyExpectedAmount(Payment payment, BigDecimal expectedAmount) {
        if (!payment.hasExpectedAmount(expectedAmount)) {
            throw new PaymentException(PaymentErrorCode.EXPECTED_AMOUNT_CONFLICT);
        }
    }

    private void validateApproval(BigDecimal expectedAmount, BigDecimal approvalAmount, String paymentKey) {
        if (expectedAmount == null || approvalAmount == null) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_AMOUNT_REQUIRED);
        }
        if (paymentKey == null || paymentKey.isBlank()) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_KEY_REQUIRED);
        }
    }

    private String resolveReason(String reason) {
        return reason == null || reason.isBlank() ? DEFAULT_FAILURE_REASON : reason.trim();
    }
}
