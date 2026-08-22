package com.example.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "expected_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal expectedAmount;

    @Column(name = "approved_amount", precision = 19, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "payment_key", unique = true, length = 100)
    private String paymentKey;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "refund_idempotency_key", unique = true, length = 150)
    private String refundIdempotencyKey;

    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Payment() {
    }

    private Payment(Long orderId, BigDecimal expectedAmount) {
        this.orderId = orderId;
        this.expectedAmount = expectedAmount;
        this.status = PaymentStatus.PENDING;
    }

    public static Payment pending(Long orderId, BigDecimal expectedAmount) {
        return new Payment(orderId, expectedAmount);
    }

    public boolean hasExpectedAmount(BigDecimal amount) {
        return amount != null && expectedAmount.compareTo(amount) == 0;
    }

    public boolean isApprovedWith(String key) {
        return status == PaymentStatus.APPROVED && paymentKey.equals(key);
    }

    public void approve(BigDecimal amount, String key) {
        this.status = PaymentStatus.APPROVED;
        this.approvedAmount = amount;
        this.paymentKey = key;
        this.failureReason = null;
    }

    public void fail(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    public boolean isRefundedWith(String idempotencyKey) {
        return status == PaymentStatus.REFUNDED && idempotencyKey.equals(refundIdempotencyKey);
    }

    public void refund(String idempotencyKey, String reason) {
        this.status = PaymentStatus.REFUNDED;
        this.refundIdempotencyKey = idempotencyKey;
        this.refundReason = reason;
        this.refundedAt = LocalDateTime.now();
    }

    public Long getOrderId() {
        return orderId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getPaymentKey() {
        return paymentKey;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public BigDecimal getExpectedAmount() {
        return expectedAmount;
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public String getRefundIdempotencyKey() {
        return refundIdempotencyKey;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }
}
