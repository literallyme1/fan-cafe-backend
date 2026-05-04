package com.example.fan_cafe.outbox.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * RabbitMQ DLQ에 적재된 메시지와 동일 내용을 조회·재처리하기 위해 DB에 보관하는 스냅샷이다.
 */
@Entity
@Table(name = "dlq_events")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DlqEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 128)
    private String eventId;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "routing_type", nullable = false, length = 32)
    private DlqRoutingType routingType;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static DlqEvent create(
            String eventId,
            String payload,
            String errorMessage,
            int retryCount,
            DlqRoutingType routingType
    ) {
        DlqEvent e = new DlqEvent();
        e.eventId = eventId;
        e.payload = payload;
        e.errorMessage = errorMessage;
        e.retryCount = retryCount;
        e.routingType = routingType;
        return e;
    }
}
