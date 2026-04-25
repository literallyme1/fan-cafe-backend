package com.example.fan_cafe.outbox.application;

public interface OutboxPublisher {
    void publish(String payload);
}
