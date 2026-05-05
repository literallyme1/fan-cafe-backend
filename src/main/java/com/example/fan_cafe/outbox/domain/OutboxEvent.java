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
@Table(name = "outbox_events")
public class OutboxEvent extends BaseTimeEntity {
    // outbox 이벤트의 최대 재시도 횟수.
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

    /**
     * 브로커/컨슈머 idempotency용 식별자. DB PK(id)와 동일한 값을 문자열로 둔다.
     * 최초 insert 직후 {@link #assignEventIdFromPrimaryKey()}로 채운다.
     */
    @Column(name = "event_id", length = 64)
    private String eventId;

    @Lob
    @Column(name = "last_error")
    private String lastError;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    // 이벤트 최초 저장 상태(NEW, retryCount=0)로 outbox 레코드를 생성한다.
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

    /** 영속화 후 {@link #id}가 배정된 뒤 한 번 호출해 event_id를 채운다. */
    public void assignEventIdFromPrimaryKey() {
        if (this.id == null) {
            throw new IllegalStateException("Outbox id must be assigned before eventId");
        }
        this.eventId = String.valueOf(this.id);
    }

    // 발행 성공 시 SENT로 전이하고 에러 정보를 초기화한다.
    public void markSent() {
        this.status = OutboxEventStatus.SENT;
        this.lastError = null;
    }

    // 발행 실패 시 재시도 상태/수동조치 상태를 한 번에 전이한다.
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
}

