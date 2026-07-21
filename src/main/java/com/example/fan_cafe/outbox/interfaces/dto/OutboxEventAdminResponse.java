package com.example.fan_cafe.outbox.interfaces.dto;

import com.example.fan_cafe.outbox.domain.OutboxEvent;
import com.example.fan_cafe.outbox.domain.OutboxEventStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record OutboxEventAdminResponse(
        @Schema(description = "Outbox 식별자", example = "12001")
        Long id,
        @Schema(description = "Aggregate 유형", example = "ORDER")
        String aggregateType,
        @Schema(description = "Aggregate 식별자", example = "10001")
        Long aggregateId,
        @Schema(description = "이벤트 JSON", example = "{\"eventId\":\"12001\",\"eventType\":\"ORDER_PAID\",\"orderId\":10001}")
        String payload,
        @Schema(description = "Outbox 상태", example = "MANUAL_REQUIRED")
        OutboxEventStatus status,
        @Schema(description = "재시도 횟수", example = "6")
        Integer retryCount,
        @Schema(description = "다음 재시도 시각", example = "2026-07-21T18:35:00", nullable = true)
        LocalDateTime nextRetryAt,
        @Schema(description = "최근 오류", example = "[MQ_TIMEOUT] publisher confirm timeout")
        String lastError,
        @Schema(description = "생성 시각", example = "2026-07-21T18:30:01")
        LocalDateTime createdAt,
        @Schema(description = "수정 시각", example = "2026-07-21T18:36:30")
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
