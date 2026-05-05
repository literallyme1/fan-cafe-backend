package com.example.fan_cafe.outbox.application;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DummyOutboxPublisher implements OutboxPublisher {

    // 실제 MQ 연동 전까지 로그 기반 더미 발행기로 동작한다.
    @Override
    public void publish(String payload, String traceId) {
        log.info("[OUTBOX DUMMY PUBLISH] traceId={} payload={}", traceId, payload);
    }
}
