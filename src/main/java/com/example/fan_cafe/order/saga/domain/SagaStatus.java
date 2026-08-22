package com.example.fan_cafe.order.saga.domain;

public enum SagaStatus {
    STARTED,
    PAYMENT_PENDING,
    PAYMENT_COMPLETED,
    COMPLETED;

    public boolean isAtOrAfter(SagaStatus milestone) {
        return ordinal() >= milestone.ordinal();
    }
}
