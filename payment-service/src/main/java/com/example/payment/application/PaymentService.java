package com.example.payment.application;

import com.example.payment.domain.Payment;
import com.example.payment.domain.PaymentStatus;
import com.example.payment.exception.PaymentErrorCode;
import com.example.payment.exception.PaymentException;
import com.example.payment.infrastructure.PaymentRepository;
import com.example.payment.interfaces.dto.PaymentResultResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PaymentService {
    private static final String DEFAULT_FAILURE_REASON = "mock payment failed";
    private final PaymentRepository paymentRepository;
    private final PaymentCreationService paymentCreationService;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentCreationService paymentCreationService
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentCreationService = paymentCreationService;
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
        if (paymentRepository.findByOrderId(orderId).isEmpty()) {
            try {
                paymentCreationService.createPending(orderId, expectedAmount);
            } catch (DataIntegrityViolationException duplicateInsert) {
                // 동일 orderId의 동시 최초 요청이 먼저 생성했다. 아래 잠금 조회로 기존 결과를 사용한다.
            }
        }
        return paymentRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_CREATION_FAILED));
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
