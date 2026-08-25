package com.example.fan_cafe.order.saga.interfaces.dto;

import com.example.fan_cafe.order.saga.domain.SagaInstance;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import com.example.fan_cafe.order.saga.domain.SagaStep;

import java.time.LocalDateTime;
import java.util.UUID;

public record SagaAdminResponse(
        UUID sagaId,
        Long orderId,
        SagaStatus status,
        SagaStep currentStep,
        int retryCount,
        LocalDateTime nextRetryAt,
        String lastError,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SagaAdminResponse from(SagaInstance saga) {
        return new SagaAdminResponse(
                saga.getSagaId(),
                saga.getOrderId(),
                saga.getStatus(),
                saga.getCurrentStep(),
                saga.getRetryCount(),
                saga.getNextRetryAt(),
                saga.getLastError(),
                saga.getCreatedAt(),
                saga.getUpdatedAt());
    }
}
