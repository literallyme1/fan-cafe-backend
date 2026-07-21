package com.example.fan_cafe.order.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Mock PG 웹훅 본문. {@code eventType}: PAYMENT_APPROVED | PAYMENT_FAILED */
@Getter
@NoArgsConstructor
public class MockPgWebhookPayload {

    @Schema(description = "PG 이벤트 유형", example = "PAYMENT_APPROVED")
    private String eventType;
    @Schema(description = "주문 식별자", example = "10001")
    private Long orderId;
    @Schema(description = "승인 금액", example = "59000")
    private BigDecimal approvalAmount;
    @Schema(description = "Mock 결제 키", example = "PAY-20260721-001")
    private String mockPaymentKey;
    @Schema(description = "결제 멱등 키", example = "PAY-20260721-001")
    private String idempotencyKey;
    @Schema(description = "결제 실패 사유", example = "카드 승인 거절")
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
