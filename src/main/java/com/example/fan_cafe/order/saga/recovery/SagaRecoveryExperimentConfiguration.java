package com.example.fan_cafe.order.saga.recovery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@Profile("experiment")
public class SagaRecoveryExperimentConfiguration {

    @Bean(destroyMethod = "shutdown")
    ExecutorService sagaRecoveryExperimentExecutor(
            @Value("${saga.recovery.experiment-concurrency:1}") int concurrency
    ) {
        if (concurrency < 1) {
            throw new IllegalArgumentException("saga.recovery.experiment-concurrency must be positive");
        }
        return Executors.newFixedThreadPool(concurrency);
    }
}
