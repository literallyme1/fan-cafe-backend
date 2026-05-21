package com.example.fan_cafe.order.interfaces.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Mock PG 웹훅 본문. {@code eventType}: PAYMENT_APPROVED | PAYMENT_FAILED */
@Getter
@NoArgsConstructor
public class MockPgWebhookPayload {

    private String eventType;
    private Long orderId;
    private BigDecimal approvalAmount;
    private String mockPaymentKey;
    private String idempotencyKey;
    private String reason;

    public String resolvePaymentKey() {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return idempotencyKey.trim();
        }
        if (mockPaymentKey != null && !mockPaymentKey.isBlank()) {
            return mockPaymentKey.trim();
        }
        return null;
    }
}
