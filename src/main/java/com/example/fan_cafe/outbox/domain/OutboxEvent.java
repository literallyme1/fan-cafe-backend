package com.example.fan_cafe.outbox.domain;

import com.example.fan_cafe.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
@Table(
        name = "outbox_events",
        indexes = {
                @Index(
                        name = "idx_outbox_polling",
                        columnList = "status, next_retry_at, id"
                )
        }
)
public class OutboxEvent extends BaseTimeEntity {
    public static final int MAX_RETRY_COUNT = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxEventStatus status;

    @Column(name = "retry_count", nullable = false)
    @Default
    private Integer retryCount = 0;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "event_id", length = 64)
    private String eventId;

    @Lob
    @Column(name = "last_error")
    private String lastError;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    public static OutboxEvent init(String aggregateType, Long aggregateId, String payload) {
        return OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .payload(payload)
                .status(OutboxEventStatus.NEW)
                .retryCount(0)
                .nextRetryAt(LocalDateTime.now())
                .lastError(null)
                .traceId(MDC.get("traceId"))
                .build();
    }

    public void assignEventIdFromPrimaryKey() {
        if (this.id == null) {
            throw new IllegalStateException("Outbox id must be assigned before eventId");
        }
        this.eventId = String.valueOf(this.id);
    }

    public void markSent() {
        this.status = OutboxEventStatus.SENT;
        this.lastError = null;
    }

    public void fail(String errorMessage, LocalDateTime nextRetryAt) {
        int nextRetryCount = this.retryCount + 1;
        this.retryCount = nextRetryCount;
        this.lastError = errorMessage;

        if (nextRetryCount > MAX_RETRY_COUNT) {
            this.status = OutboxEventStatus.MANUAL_REQUIRED;
            this.nextRetryAt = null;
            return;
        }

        this.status = OutboxEventStatus.FAILED;
        this.nextRetryAt = nextRetryAt;
    }

    public boolean isManualRequired() {
        return this.status == OutboxEventStatus.MANUAL_REQUIRED;
    }

    public void markManualRetryRequested(LocalDateTime now) {
        if (!isManualRequired()) {
            throw new IllegalStateException("Outbox event is not MANUAL_REQUIRED.");
        }
        this.status = OutboxEventStatus.FAILED;
        this.nextRetryAt = now;
    }
}
