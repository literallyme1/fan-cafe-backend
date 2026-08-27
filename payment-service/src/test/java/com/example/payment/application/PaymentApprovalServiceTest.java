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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentApprovalServiceTest {
    private static final Long ORDER_ID = 10L;
    private static final BigDecimal AMOUNT = new BigDecimal("20000.00");

    @Mock private PaymentRepository paymentRepository;
    @Mock private Clock clock;
    @InjectMocks private PaymentApprovalService paymentApprovalService;

    @org.junit.jupiter.api.BeforeEach
    void setUpClock() {
        org.mockito.Mockito.lenient().when(clock.instant())
                .thenReturn(Instant.parse("2026-08-27T00:00:00Z"));
        org.mockito.Mockito.lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void pendingPayment_isApproved() {
        Payment payment = Payment.pending(ORDER_ID, AMOUNT);
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));

        var result = paymentApprovalService.approve(ORDER_ID, AMOUNT, AMOUNT, "pay-1");

        assertThat(result.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.paymentKey()).isEqualTo("pay-1");
    }

    @Test
    void duplicateApprovalWithSameKey_returnsExistingResult() {
        Payment payment = Payment.pending(ORDER_ID, AMOUNT);
        payment.approve(AMOUNT, "pay-1");
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));

        var result = paymentApprovalService.approve(ORDER_ID, AMOUNT, AMOUNT, "pay-1");

        assertThat(result.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.paymentKey()).isEqualTo("pay-1");
    }

    @Test
    void duplicateApprovalWithDifferentKey_isRejected() {
        Payment payment = Payment.pending(ORDER_ID, AMOUNT);
        payment.approve(AMOUNT, "pay-1");
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentApprovalService.approve(ORDER_ID, AMOUNT, AMOUNT, "pay-2"))
                .isInstanceOf(PaymentException.class)
                .extracting(exception -> ((PaymentException) exception).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_ALREADY_APPROVED);
    }

    @Test
    void missingPaymentAfterCreationAttempt_isReportedAsCreationFailure() {
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentApprovalService.approve(ORDER_ID, AMOUNT, AMOUNT, "pay-1"))
                .isInstanceOf(PaymentException.class)
                .extracting(exception -> ((PaymentException) exception).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_CREATION_FAILED);
    }
}
