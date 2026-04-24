package com.example.fan_cafe.outbox.domain;

public enum OutboxEventStatus {
    // 신규 저장되어 아직 발행되지 않은 상태.
    NEW,
    // 브로커 발행에 성공한 상태.
    SENT,
    // 발행 실패했지만 재시도 가능한 상태.
    FAILED,
    // 재시도 한도를 넘겨 수동 조치가 필요한 상태.
    MANUAL_REQUIRED
}

