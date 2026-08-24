package com.example.fan_cafe.order.saga.recovery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@Profile("experiment")
public class SagaRecoveryExperimentMeasurement implements SagaRecoveryClaimObserver {
    private static final String BEFORE = "BEFORE";

    private final JdbcTemplate jdbcTemplate;
    private final String runId;
    private final AtomicBoolean recorded = new AtomicBoolean();

    public SagaRecoveryExperimentMeasurement(
            JdbcTemplate jdbcTemplate,
            @Value("${saga.recovery.experiment-run-id:}") String runId
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.runId = runId;
    }

    @Override
    public void claimSucceeded(SagaRecoveryClaim claim) {
        if (runId.isBlank() || !recorded.compareAndSet(false, true)) {
            return;
        }

        try {
            int updated = jdbcTemplate.update("""
                    UPDATE saga_experiment_snapshot
                    SET worker_started_at = COALESCE(worker_started_at, NOW(6))
                    WHERE run_id = ?
                      AND snapshot_phase = ?
                    """, runId, BEFORE);
            if (updated == 0) {
                recorded.set(false);
                log.warn("[SAGA EXPERIMENT] BEFORE snapshot is missing runId={}", runId);
            }
        } catch (RuntimeException measurementFailure) {
            recorded.set(false);
            log.warn("[SAGA EXPERIMENT] failed to record first successful claim runId={}",
                    runId, measurementFailure);
        }
    }
}
