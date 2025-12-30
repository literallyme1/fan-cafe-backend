package com.example.fan_cafe.notification.infrastructure.messaging.consumer;

import com.example.fan_cafe.notification.application.retry.RetryPolicy;
import com.example.fan_cafe.notification.infrastructure.messaging.retry.RetryRouter;
import com.example.fan_cafe.notification.infrastructure.messaging.retry.XDeathHeaderReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final RetryPolicy retryPolicy;
    private final XDeathHeaderReader xDeathHeaderReader;
    private final RetryRouter retryRouter;

}
