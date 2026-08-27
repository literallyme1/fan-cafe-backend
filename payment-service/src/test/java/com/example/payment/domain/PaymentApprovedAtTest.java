package com.example.payment.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentApprovedAtTest {

    @Test
    void approvedAtIsAssignedOnceAndSurvivesLaterStateChanges() {
        Payment payment = Payment.pending(100L, new BigDecimal("10000"));
        payment.approve(new BigDecimal("10000"), "payment-key");
        LocalDateTime firstApprovedAt = payment.getApprovedAt();

        payment.approve(new BigDecimal("10000"), "payment-key");
        payment.refund("REFUND:" + UUID.randomUUID(), "campaign failed");

        assertThat(firstApprovedAt).isNotNull();
        assertThat(payment.getApprovedAt()).isEqualTo(firstApprovedAt);
        assertThat(payment.getRefundedAt()).isNotNull();
    }
}
