package com.example.payment.application;

import com.example.payment.PaymentServiceApplication;
import com.example.payment.domain.PaymentStatus;
import com.example.payment.infrastructure.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentCrashRecoveryIntegrationTest {
    private static final String DB_URL =
            "jdbc:h2:mem:payment-crash-recovery;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
    private static final Long ORDER_ID = 970_001L;
    private static final BigDecimal AMOUNT = new BigDecimal("20000.00");
    private static final UUID SAGA_ID =
            UUID.fromString("550e8400-e29b-41d4-a716-446655440007");

    @Test
    void duplicateRefundAfterFreshContextUsesPersistedIdempotencyWithoutSecondTransition() {
        LocalDateTime firstRefundedAt;
        long versionAfterFirstRefund;

        try (ConfigurableApplicationContext executionA = startContext("create")) {
            PaymentService paymentService = executionA.getBean(PaymentService.class);
            paymentService.approve(ORDER_ID, AMOUNT, AMOUNT, "crash-payment-key");
            paymentService.refund(ORDER_ID, SAGA_ID, "first command before result loss");
            firstRefundedAt = paymentRefundedAt(executionA);
            versionAfterFirstRefund = paymentVersion(executionA);
        }

        try (ConfigurableApplicationContext executionB = startContext("validate")) {
            PaymentService paymentService = executionB.getBean(PaymentService.class);
            var duplicate = paymentService.refund(ORDER_ID, SAGA_ID, "reissued command after restart");

            assertThat(duplicate.status()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(duplicate.refundIdempotencyKey()).isEqualTo("REFUND:" + SAGA_ID);
            assertThat(duplicate.refundedAt()).isEqualTo(firstRefundedAt);
            assertThat(duplicate.refundReason()).isEqualTo("first command before result loss");
            assertThat(paymentVersion(executionB)).isEqualTo(versionAfterFirstRefund);
            assertThat(executionB.getBean(PaymentRepository.class).count()).isEqualTo(1);
        }
    }

    private ConfigurableApplicationContext startContext(String ddlMode) {
        return new SpringApplicationBuilder(PaymentServiceApplication.class)
                .web(WebApplicationType.NONE)
                .run(
                        "--spring.datasource.url=" + DB_URL,
                        "--spring.datasource.driver-class-name=org.h2.Driver",
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password=",
                        "--spring.jpa.hibernate.ddl-auto=" + ddlMode,
                        "--spring.flyway.enabled=false",
                        "--spring.sql.init.mode=never",
                        "--spring.rabbitmq.listener.simple.auto-startup=false",
                        "--spring.rabbitmq.listener.direct.auto-startup=false"
                );
    }

    private long paymentVersion(ConfigurableApplicationContext context) {
        Long version = context.getBean(JdbcTemplate.class).queryForObject(
                "SELECT version FROM payments WHERE order_id = ?", Long.class, ORDER_ID);
        return version == null ? -1L : version;
    }

    private LocalDateTime paymentRefundedAt(ConfigurableApplicationContext context) {
        return context.getBean(JdbcTemplate.class).queryForObject(
                "SELECT refunded_at FROM payments WHERE order_id = ?",
                LocalDateTime.class,
                ORDER_ID);
    }
}
