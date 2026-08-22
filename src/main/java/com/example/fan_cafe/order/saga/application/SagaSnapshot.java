package com.example.fan_cafe.order.saga.application;

import com.example.fan_cafe.order.saga.domain.SagaInstance;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import com.example.fan_cafe.order.saga.domain.SagaStep;

import java.util.UUID;

public record SagaSnapshot(
        UUID sagaId,
        Long orderId,
        SagaStatus status,
        SagaStep currentStep
) {
    public static SagaSnapshot from(SagaInstance saga) {
        return new SagaSnapshot(saga.getSagaId(), saga.getOrderId(), saga.getStatus(), saga.getCurrentStep());
    }
}
