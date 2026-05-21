package com.example.fan_cafe.order.interfaces.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class MockPaymentApproveRequest {

    @NotNull(message = "승인 금액은 필수입니다.")
    private BigDecimal approvalAmount;

    /** Mock PG 결제 식별자 (idempotencyKey 없을 때 사용) */
    private String mockPaymentKey;

    /** 중복 승인 요청 식별용 — mockPaymentKey보다 우선 */
    private String idempotencyKey;

    // 멱등 키 하나로 통일: idempotencyKey → mockPaymentKey 순.
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
