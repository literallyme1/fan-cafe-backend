package com.example.fan_cafe.outbox.interfaces.dto;

import com.example.fan_cafe.outbox.domain.OutboxEvent;
import com.example.fan_cafe.outbox.domain.OutboxEventStatus;

import java.time.LocalDateTime;

public record OutboxEventAdminResponse(
        Long id,
        String aggregateType,
        Long aggregateId,
        String payload,
        OutboxEventStatus status,
        Integer retryCount,
        LocalDateTime nextRetryAt,
        String lastError,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static OutboxEventAdminResponse from(OutboxEvent event) {
        return new OutboxEventAdminResponse(
                event.getId(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getPayload(),
                event.getStatus(),
                event.getRetryCount(),
                event.getNextRetryAt(),
                event.getLastError(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
