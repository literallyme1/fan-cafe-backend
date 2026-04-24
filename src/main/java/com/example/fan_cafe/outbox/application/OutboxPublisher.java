package com.example.fan_cafe.outbox.application;

import com.example.fan_cafe.outbox.domain.OutboxEvent;

public interface OutboxPublisher {
    void publish(OutboxEvent event);
}
