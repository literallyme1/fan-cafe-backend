package com.example.fan_cafe.outbox.domain;

import com.example.fan_cafe.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "next_retry_at", nullable = false)
    private LocalDateTime nextRetryAt;

    @Lob
    @Column(name = "last_error")
    private String lastError;

    // 이벤트 최초 저장 상태(INIT, retryCount=0)로 outbox 레코드를 생성한다.
    public static OutboxEvent init(String aggregateType, Long aggregateId, String payload) {
        return OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .payload(payload)
                .status(OutboxEventStatus.INIT)
                .retryCount(0)
                .nextRetryAt(LocalDateTime.now())
                .lastError(null)
                .build();
    }
}

