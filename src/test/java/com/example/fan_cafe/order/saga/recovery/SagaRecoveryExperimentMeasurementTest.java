package com.example.fan_cafe.order.saga.recovery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SagaRecoveryExperimentMeasurementTest {
    @Mock private JdbcTemplate jdbcTemplate;

    @Test
    void concurrentSuccessfulClaimsRecordWorkerStartOnlyOnce() throws Exception {
        String runId = "concurrency-10-run-1";
        SagaRecoveryExperimentMeasurement measurement =
                new SagaRecoveryExperimentMeasurement(jdbcTemplate, runId);
        when(jdbcTemplate.update(anyString(), eq(runId), eq("BEFORE"))).thenReturn(1);
        int concurrency = 10;
        CountDownLatch ready = new CountDownLatch(concurrency);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(concurrency);

        try {
            for (int index = 0; index < concurrency; index++) {
                executor.submit(() -> {
                    ready.countDown();
                    start.await(10, TimeUnit.SECONDS);
                    measurement.claimSucceeded(null);
                    return null;
                });
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        verify(jdbcTemplate).update(anyString(), eq(runId), eq("BEFORE"));
    }

    @Test
    void blankRunIdDoesNotWriteMeasurement() {
        SagaRecoveryExperimentMeasurement measurement =
                new SagaRecoveryExperimentMeasurement(jdbcTemplate, " ");

        measurement.claimSucceeded(null);

        verify(jdbcTemplate, never()).update(anyString(), eq(" "), eq("BEFORE"));
    }
}
