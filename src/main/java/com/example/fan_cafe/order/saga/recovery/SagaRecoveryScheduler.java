package com.example.fan_cafe.order.saga.recovery;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!experiment")
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "saga.recovery.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SagaRecoveryScheduler {
    private final SagaRecoveryWorker worker;

    @Scheduled(fixedDelayString = "${saga.recovery.fixed-delay:5s}")
    public void recoverDueSagas() {
        worker.recoverDueSagas();
    }
}
