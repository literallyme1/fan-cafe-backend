package com.example.fan_cafe.outbox.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "processed_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_processed_events_event_id_consumer_type",
                columnNames = {"event_id", "consumer_type"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "consumer_type", nullable = false, length = 64)
    private String consumerType;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    public static ProcessedEvent record(String eventId, String consumerType) {
        ProcessedEvent entity = new ProcessedEvent();
        entity.eventId = eventId;
        entity.consumerType = consumerType;
        entity.processedAt = LocalDateTime.now();
        return entity;
    }
}
