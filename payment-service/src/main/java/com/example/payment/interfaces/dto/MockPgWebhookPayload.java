package com.example.payment.interfaces.dto;

import java.math.BigDecimal;

public class MockPgWebhookPayload {
    private String eventType;
    private Long orderId;
    private BigDecimal approvalAmount;
    private String mockPaymentKey;
    private String idempotencyKey;
    private String reason;

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public BigDecimal getApprovalAmount() { return approvalAmount; }
    public void setApprovalAmount(BigDecimal approvalAmount) { this.approvalAmount = approvalAmount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public void setMockPaymentKey(String mockPaymentKey) { this.mockPaymentKey = mockPaymentKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String resolvePaymentKey() {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) return idempotencyKey.trim();
        if (mockPaymentKey != null && !mockPaymentKey.isBlank()) return mockPaymentKey.trim();
        return null;
    }
}
