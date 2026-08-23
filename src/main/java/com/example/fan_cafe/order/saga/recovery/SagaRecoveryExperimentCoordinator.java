package com.example.fan_cafe.order.saga.recovery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Slf4j
@Component
@Profile("experiment")
@ConditionalOnProperty(
        name = "saga.recovery.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SagaRecoveryExperimentCoordinator {
    private final SagaRecoveryWorker worker;
    private final ExecutorService executor;
    private final int concurrency;

    public SagaRecoveryExperimentCoordinator(
            SagaRecoveryWorker worker,
            @Qualifier("sagaRecoveryExperimentExecutor") ExecutorService executor,
            @Value("${saga.recovery.experiment-concurrency:1}") int concurrency
    ) {
        this.worker = worker;
        this.executor = executor;
        this.concurrency = concurrency;
    }

    @Scheduled(fixedDelayString = "${saga.recovery.fixed-delay:5s}")
    public void recoverDueSagasConcurrently() {
        List<Callable<Void>> lanes = new ArrayList<>(concurrency);
        for (int lane = 0; lane < concurrency; lane++) {
            lanes.add(() -> {
                worker.recoverDueSagas();
                return null;
            });
        }

        try {
            List<Future<Void>> results = executor.invokeAll(lanes);
            for (Future<Void> result : results) {
                reportLaneFailure(result);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            log.warn("[SAGA EXPERIMENT] recovery coordinator interrupted", interrupted);
        }
    }

    private void reportLaneFailure(Future<Void> result) {
        try {
            result.get();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            log.warn("[SAGA EXPERIMENT] recovery lane interrupted", interrupted);
        } catch (ExecutionException failure) {
            log.error("[SAGA EXPERIMENT] recovery lane failed", failure.getCause());
        }
    }
}
