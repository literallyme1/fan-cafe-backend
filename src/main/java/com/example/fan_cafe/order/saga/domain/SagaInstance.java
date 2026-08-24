package com.example.fan_cafe.order.saga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(
        name = "saga_instance",
        indexes = @Index(
                name = "idx_saga_recovery",
                columnList = "status, next_retry_at"
        )
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SagaInstance {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "saga_id", nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private UUID sagaId;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SagaStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", nullable = false, length = 40)
    private SagaStep currentStep;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "payment_unknown_at")
    private LocalDateTime paymentUnknownAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private SagaInstance(UUID sagaId, Long orderId) {
        this.sagaId = sagaId;
        this.orderId = orderId;
        this.status = SagaStatus.STARTED;
        this.currentStep = SagaStep.PAYMENT_APPROVAL;
        this.retryCount = 0;
    }

    public static SagaInstance started(Long orderId) {
        return new SagaInstance(UUID.randomUUID(), orderId);
    }

    void changeState(SagaStatus status, SagaStep currentStep, LocalDateTime transitionedAt) {
        SagaStatus previousStatus = this.status;
        this.status = status;
        this.currentStep = currentStep;
        if (previousStatus == SagaStatus.PAYMENT_PENDING
                && status == SagaStatus.PAYMENT_UNKNOWN
                && paymentUnknownAt == null) {
            this.paymentUnknownAt = transitionedAt;
        }
        if (isTerminal(status) && resolvedAt == null) {
            this.resolvedAt = transitionedAt;
        }
        if (isRecoveryTarget(previousStatus)
                && !isRecoveryTarget(status)
                && status != SagaStatus.RECONCILIATION_REQUIRED) {
            this.nextRetryAt = null;
            this.lastError = null;
        }
    }

    public void schedulePaymentUnknownRecovery(LocalDateTime firstRecoveryAt, String errorSummary) {
        if (status != SagaStatus.PAYMENT_UNKNOWN) {
            throw new IllegalStateException("Payment recovery can only be scheduled for PAYMENT_UNKNOWN");
        }
        this.nextRetryAt = requireRecoveryTime(firstRecoveryAt);
        this.lastError = errorSummary;
    }

    public void scheduleInitialRefundResultDeadline(LocalDateTime refundResultDeadline) {
        if (status != SagaStatus.COMPENSATING) {
            throw new IllegalStateException("Refund result deadline can only be set for COMPENSATING");
        }
        this.nextRetryAt = requireRecoveryTime(refundResultDeadline);
        this.lastError = null;
    }

    public void claimRecoveryUntil(LocalDateTime claimLeaseUntil) {
        if (!isRecoveryTarget(status)) {
            throw new IllegalStateException("Only an unfinished Saga can be claimed for recovery");
        }
        this.nextRetryAt = requireRecoveryTime(claimLeaseUntil).truncatedTo(ChronoUnit.MICROS);
    }

    public boolean hasClaim(SagaStatus claimedStatus, LocalDateTime claimedUntil) {
        return status == claimedStatus && claimedUntil != null && claimedUntil.equals(nextRetryAt);
    }

    public void recordRecoveryFailure(int failureCount, LocalDateTime retryAt, String errorSummary) {
        if (!isRecoveryTarget(status)) {
            throw new IllegalStateException("Recovery failure requires an unfinished Saga");
        }
        this.retryCount = failureCount;
        this.nextRetryAt = requireRecoveryTime(retryAt);
        this.lastError = errorSummary;
    }

    public void recordReconciliationFailure(int failureCount, String errorSummary) {
        if (status != SagaStatus.RECONCILIATION_REQUIRED) {
            throw new IllegalStateException("Reconciliation metadata requires terminal Saga status");
        }
        this.retryCount = failureCount;
        this.nextRetryAt = null;
        this.lastError = errorSummary;
    }

    private LocalDateTime requireRecoveryTime(LocalDateTime recoveryTime) {
        if (recoveryTime == null) {
            throw new IllegalArgumentException("Recovery time is required");
        }
        return recoveryTime;
    }

    private boolean isRecoveryTarget(SagaStatus sagaStatus) {
        return sagaStatus == SagaStatus.PAYMENT_UNKNOWN || sagaStatus == SagaStatus.COMPENSATING;
    }

    private boolean isTerminal(SagaStatus sagaStatus) {
        return sagaStatus == SagaStatus.COMPLETED
                || sagaStatus == SagaStatus.CANCELLED
                || sagaStatus == SagaStatus.COMPENSATED
                || sagaStatus == SagaStatus.RECONCILIATION_REQUIRED;
    }
}
