package com.example.fan_cafe.outbox.domain;

public enum OutboxEventStatus {
    // 발행 대기 상태.
    INIT,
    // 브로커 발행이 성공한 상태.
    PROCESSED,
    // 발행 실패 상태(재시도 대상).
    FAILED
}

