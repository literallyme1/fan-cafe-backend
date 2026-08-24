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
public class PaymentApprovalService {
    private final PaymentRepository paymentRepository;

    public PaymentApprovalService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public PaymentResultResponse approve(
            Long orderId,
            BigDecimal expectedAmount,
            BigDecimal approvalAmount,
            String paymentKey
    ) {
        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_CREATION_FAILED));
        verifyExpectedAmount(payment, expectedAmount);

        if (payment.getStatus() == PaymentStatus.APPROVED) {
            if (payment.isApprovedWith(paymentKey)) {
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

        payment.approve(approvalAmount, paymentKey);
        return PaymentResultResponse.from(payment);
    }

    private void verifyExpectedAmount(Payment payment, BigDecimal expectedAmount) {
        if (!payment.hasExpectedAmount(expectedAmount)) {
            throw new PaymentException(PaymentErrorCode.EXPECTED_AMOUNT_CONFLICT);
        }
    }
}
