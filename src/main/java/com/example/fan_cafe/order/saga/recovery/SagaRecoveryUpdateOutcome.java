package com.example.fan_cafe.order.saga.recovery;

public enum SagaRecoveryUpdateOutcome {
    RETRY_SCHEDULED,
    RECONCILIATION_REQUIRED,
    STALE_CLAIM
}
