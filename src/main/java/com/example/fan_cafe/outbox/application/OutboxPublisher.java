package com.example.fan_cafe.outbox.application;

public interface OutboxPublisher {

    default void publish(String payload) {
        publish(payload, null);
    }

    /**
     * @param traceId 저장된 추적 ID; 배치 Poller 등 MDC가 비어 있을 때 헤더 전파용. null이면 {@link org.slf4j.MDC} 보조.
     */
    void publish(String payload, String traceId);
}
