package com.example.payment.payment;

import com.example.payment.config.MockPgProperties;
import com.example.payment.exception.PaymentErrorCode;
import com.example.payment.exception.PaymentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockPgWebhookSignatureVerifierTest {
    private static final String SECRET = "test-secret";
    private MockPgWebhookSignatureVerifier verifier;

    @BeforeEach
    void setUp() {
        MockPgProperties properties = new MockPgProperties();
        properties.setWebhookSecret(SECRET);
        verifier = new MockPgWebhookSignatureVerifier(properties);
    }

    @Test
    void validSignature_isAccepted() {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String body = "{\"orderId\":10}";
        verifier.verify(timestamp, body, MockPgWebhookSignatureVerifier.sign(SECRET, timestamp, body));
    }

    @Test
    void invalidSignature_isRejected() {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        assertThatThrownBy(() -> verifier.verify(timestamp, "{}", "invalid"))
                .isInstanceOf(PaymentException.class)
                .extracting(exception -> ((PaymentException) exception).getErrorCode())
                .isEqualTo(PaymentErrorCode.WEBHOOK_SIGNATURE_INVALID);
    }

    @Test
    void expiredTimestamp_isRejected() {
        String timestamp = String.valueOf(Instant.now().minusSeconds(301).getEpochSecond());
        String body = "{}";
        assertThatThrownBy(() -> verifier.verify(
                timestamp, body, MockPgWebhookSignatureVerifier.sign(SECRET, timestamp, body)))
                .isInstanceOf(PaymentException.class)
                .extracting(exception -> ((PaymentException) exception).getErrorCode())
                .isEqualTo(PaymentErrorCode.WEBHOOK_TIMESTAMP_EXPIRED);
    }
}
