package com.example.fan_cafe.outbox.domain;

public enum OutboxErrorCode {
    MQ_TIMEOUT,
    MQ_CONNECTION_ERROR,
    MQ_SERIALIZATION_ERROR,
    MQ_UNKNOWN_ERROR
}
