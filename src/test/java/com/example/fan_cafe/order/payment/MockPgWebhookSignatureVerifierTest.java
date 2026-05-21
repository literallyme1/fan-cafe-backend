package com.example.fan_cafe.order.payment;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.config.MockPgProperties;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockPgWebhookSignatureVerifierTest {

    private static final String SECRET = "test-mock-pg-webhook-secret";

    private MockPgWebhookSignatureVerifier verifier;

    @BeforeEach
    void setUp() {
        MockPgProperties properties = new MockPgProperties();
        properties.setWebhookSecret(SECRET);
        verifier = new MockPgWebhookSignatureVerifier(properties);
    }

    @Test
    @DisplayName("유효한 HMAC 서명과 타임스탬프면 검증에 통과한다.")
    void verify_shouldPass_whenSignatureValid() {
        String rawBody = "{\"eventType\":\"PAYMENT_APPROVED\",\"orderId\":1}";
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = MockPgWebhookSignatureVerifier.signForTest(SECRET, timestamp, rawBody);

        assertThatCode(() -> verifier.verify(timestamp, rawBody, signature))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("서명이 다르면 WEBHOOK_SIGNATURE_INVALID 예외가 발생한다.")
    void verify_shouldThrow_whenSignatureMismatch() {
        String rawBody = "{\"eventType\":\"PAYMENT_APPROVED\"}";
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        assertThatThrownBy(() -> verifier.verify(timestamp, rawBody, "deadbeef"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(OrderErrorCode.WEBHOOK_SIGNATURE_INVALID.getMessage());
    }

    @Test
    @DisplayName("타임스탬프가 5분 이상 지나면 WEBHOOK_TIMESTAMP_EXPIRED 예외가 발생한다.")
    void verify_shouldThrow_whenTimestampExpired() {
        String rawBody = "{}";
        String timestamp = String.valueOf(Instant.now().minusSeconds(400).getEpochSecond());
        String signature = MockPgWebhookSignatureVerifier.signForTest(SECRET, timestamp, rawBody);

        assertThatThrownBy(() -> verifier.verify(timestamp, rawBody, signature))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(OrderErrorCode.WEBHOOK_TIMESTAMP_EXPIRED.getMessage());
    }
}
