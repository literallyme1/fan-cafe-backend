package com.example.payment.application;

import com.example.payment.config.MockPgProperties;
import com.example.payment.domain.PaymentStatus;
import com.example.payment.exception.PaymentErrorCode;
import com.example.payment.exception.PaymentException;
import com.example.payment.interfaces.dto.PaymentResultResponse;
import com.example.payment.payment.MockPgWebhookSignatureVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MockPgWebhookServiceTest {
    private static final String SECRET = "test-secret";
    @Mock private PaymentService paymentService;
    private MockPgWebhookService webhookService;

    @BeforeEach
    void setUp() {
        MockPgProperties properties = new MockPgProperties();
        properties.setWebhookSecret(SECRET);
        webhookService = new MockPgWebhookService(
                new MockPgWebhookSignatureVerifier(properties), paymentService, new ObjectMapper());
    }

    @Test
    void approvedWebhook_isVerifiedAndDelegated() {
        String body = "{\"eventType\":\"PAYMENT_APPROVED\",\"orderId\":10,"
                + "\"approvalAmount\":20000,\"idempotencyKey\":\"key-1\"}";
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = MockPgWebhookSignatureVerifier.sign(SECRET, timestamp, body);
        PaymentResultResponse expected = new PaymentResultResponse(
                10L, PaymentStatus.APPROVED, "key-1", null, null);
        when(paymentService.approve(10L, BigDecimal.valueOf(20000),
                BigDecimal.valueOf(20000), "key-1")).thenReturn(expected);

        var result = webhookService.receive(
                body, timestamp, signature, BigDecimal.valueOf(20000));

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void invalidSignature_doesNotCallPaymentService() {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        assertThatThrownBy(() -> webhookService.receive("{}", timestamp, "invalid", BigDecimal.TEN))
                .isInstanceOf(PaymentException.class)
                .extracting(exception -> ((PaymentException) exception).getErrorCode())
                .isEqualTo(PaymentErrorCode.WEBHOOK_SIGNATURE_INVALID);
        verifyNoInteractions(paymentService);
    }
}
