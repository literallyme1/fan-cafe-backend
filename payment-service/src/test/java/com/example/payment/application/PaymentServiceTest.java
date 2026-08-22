package com.example.payment.application;

import com.example.payment.domain.Payment;
import com.example.payment.domain.PaymentStatus;
import com.example.payment.exception.PaymentErrorCode;
import com.example.payment.exception.PaymentException;
import com.example.payment.infrastructure.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock private PaymentRepository paymentRepository;
    @InjectMocks private PaymentService paymentService;

    @Test
    void approve_persistsApprovedPayment() {
        when(paymentRepository.findByOrderIdForUpdate(10L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = paymentService.approve(
                10L, new BigDecimal("20000.00"), new BigDecimal("20000.00"), "pay-1");

        assertThat(result.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.paymentKey()).isEqualTo("pay-1");
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void duplicateApprovalWithSameKey_isIdempotent() {
        Payment payment = Payment.pending(10L, BigDecimal.valueOf(20000));
        payment.approve(BigDecimal.valueOf(20000), "pay-1");
        when(paymentRepository.findByOrderIdForUpdate(10L)).thenReturn(Optional.of(payment));

        var result = paymentService.approve(
                10L, BigDecimal.valueOf(20000), BigDecimal.valueOf(20000), "pay-1");

        assertThat(result.status()).isEqualTo(PaymentStatus.APPROVED);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void approvalAmountMismatch_persistsFailedResult() {
        when(paymentRepository.findByOrderIdForUpdate(10L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = paymentService.approve(
                10L, BigDecimal.valueOf(20000), BigDecimal.ONE, "pay-1");

        assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.failureCode()).isEqualTo("PAYMENT_AMOUNT_MISMATCH");
    }

    @Test
    void secondApprovalWithDifferentKey_isRejected() {
        Payment payment = Payment.pending(10L, BigDecimal.valueOf(20000));
        payment.approve(BigDecimal.valueOf(20000), "pay-1");
        when(paymentRepository.findByOrderIdForUpdate(10L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.approve(
                10L, BigDecimal.valueOf(20000), BigDecimal.valueOf(20000), "pay-2"))
                .isInstanceOf(PaymentException.class)
                .extracting(exception -> ((PaymentException) exception).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_ALREADY_APPROVED);
    }

    @Test
    void fail_isIdempotent() {
        Payment payment = Payment.pending(10L, BigDecimal.valueOf(20000));
        payment.fail("declined");
        when(paymentRepository.findByOrderIdForUpdate(10L)).thenReturn(Optional.of(payment));

        var result = paymentService.fail(10L, BigDecimal.valueOf(20000), "retry");

        assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo("declined");
    }
}
