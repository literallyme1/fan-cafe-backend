package com.example.payment.application;

import com.example.payment.domain.PaymentStatus;
import com.example.payment.infrastructure.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.hikari.maximum-pool-size=1",
                "spring.datasource.hikari.connection-timeout=500"
        }
)
class PaymentApprovalTransactionBoundaryIntegrationTest {
    @Autowired private PaymentService paymentService;
    @Autowired private PaymentRepository paymentRepository;

    @AfterEach
    void clearPayments() {
        paymentRepository.deleteAll();
    }

    @Test
    void firstApprovalCompletesWithSingleConnectionPool() {
        BigDecimal amount = new BigDecimal("20000.00");

        var result = paymentService.approve(910_001L, amount, amount, "single-connection-key");

        assertThat(result.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(paymentRepository.findByOrderId(910_001L))
                .get()
                .extracting(payment -> payment.getStatus())
                .isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    void duplicateApprovalKeepsOriginalIdempotentResultWithSingleConnectionPool() {
        BigDecimal amount = new BigDecimal("20000.00");

        var first = paymentService.approve(910_002L, amount, amount, "same-key");
        var duplicate = paymentService.approve(910_002L, amount, amount, "same-key");

        assertThat(duplicate.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(duplicate.paymentKey()).isEqualTo(first.paymentKey());
        assertThat(paymentRepository.count()).isEqualTo(1);
    }
}
