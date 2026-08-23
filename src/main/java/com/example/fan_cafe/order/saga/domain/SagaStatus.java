package com.example.fan_cafe.order.saga.domain;

public enum SagaStatus {
    STARTED,
    PAYMENT_PENDING,
    PAYMENT_UNKNOWN,
    PAYMENT_COMPLETED,
    COMPENSATING,
    COMPLETED,
    CANCELLED,
    COMPENSATED,
    RECONCILIATION_REQUIRED;

    public boolean isAtOrAfter(SagaStatus milestone) {
        return switch (milestone) {
            case STARTED -> this == STARTED
                    || this == PAYMENT_PENDING
                    || this == PAYMENT_COMPLETED
                    || this == COMPLETED;
            case PAYMENT_PENDING -> this == PAYMENT_PENDING
                    || this == PAYMENT_COMPLETED
                    || this == COMPLETED;
            case PAYMENT_COMPLETED -> this == PAYMENT_COMPLETED || this == COMPLETED;
            case COMPLETED -> this == COMPLETED;
            case PAYMENT_UNKNOWN, COMPENSATING, CANCELLED, COMPENSATED,
                    RECONCILIATION_REQUIRED -> this == milestone;
        };
    }
}
