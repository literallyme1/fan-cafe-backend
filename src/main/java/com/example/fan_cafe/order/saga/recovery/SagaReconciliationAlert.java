package com.example.fan_cafe.order.saga.recovery;

import com.example.fan_cafe.order.saga.domain.SagaStatus;

import java.util.UUID;

public record SagaReconciliationAlert(
        String eventType,
        UUID sagaId,
        Long orderId,
        SagaStatus previousStatus,
        int retryCount,
        String lastError,
        String eventId
) {
    public static final String EVENT_TYPE = "SAGA_RECONCILIATION_REQUIRED";

    public static SagaReconciliationAlert of(
            UUID sagaId,
            Long orderId,
            SagaStatus previousStatus,
            int retryCount,
            String lastError
    ) {
        return new SagaReconciliationAlert(
                EVENT_TYPE, sagaId, orderId, previousStatus, retryCount, lastError, null);
    }
}
