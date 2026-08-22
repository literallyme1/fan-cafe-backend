package com.example.payment.infrastructure;

import com.example.payment.domain.Payment;
import com.example.payment.PaymentServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = PaymentServiceApplication.class)
class PaymentRepositoryIntegrationTest {
    @Autowired private PaymentRepository paymentRepository;

    @Test
    void paymentIsStoredInPaymentDatabaseSchema() {
        Payment payment = Payment.pending(100L, new BigDecimal("59000.00"));
        payment.approve(new BigDecimal("59000.00"), "payment-100");

        paymentRepository.saveAndFlush(payment);

        Payment reloaded = paymentRepository.findByOrderId(100L).orElseThrow();
        assertThat(reloaded.getPaymentKey()).isEqualTo("payment-100");
    }
}
