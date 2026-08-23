package com.example.fan_cafe.order.saga.recovery;

public interface SagaRecoveryClaimObserver {
    void claimSucceeded(SagaRecoveryClaim claim);
}
