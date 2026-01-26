package com.example.fan_cafe.global.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

//Notification 전용 Executor (비즈니스 로직과 분리)
@Configuration
public class NotificationExecutorConfig {

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        return Executors.newSingleThreadExecutor();
    }
}
