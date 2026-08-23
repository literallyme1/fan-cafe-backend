package com.example.fan_cafe.order.saga.recovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class SagaRecoveryConfiguration {
    @Bean
    Clock sagaRecoveryClock() {
        return Clock.systemDefaultZone();
    }
}
