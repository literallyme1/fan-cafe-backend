package com.example.fan_cafe.order.saga.recovery;

import com.example.fan_cafe.order.saga.domain.SagaInstance;
import com.example.fan_cafe.order.saga.domain.SagaStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SagaRecoveryClaim(
        UUID sagaId,
        Long orderId,
        SagaStatus status,
        int retryCount,
        LocalDateTime claimedUntil
) {
    static SagaRecoveryClaim from(SagaInstance saga, LocalDateTime claimedUntil) {
        return new SagaRecoveryClaim(
                saga.getSagaId(), saga.getOrderId(), saga.getStatus(),
                saga.getRetryCount(), claimedUntil);
    }
}
