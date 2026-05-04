package com.example.fan_cafe.outbox.mq;

/** Outbox 소비·재발행 시 재시도 차수를 RabbitMQ 메시지 헤더로만 전달하기 위한 상수. */
public final class OutboxMqRetryHeaders {

    /**
     * 소비 측에서 몇 번째 재처리인지 나타낸다.
     * 헤더가 없으면 0으로 간주한다.
     */
    public static final String X_RETRY_COUNT = "x-retry-count";

    private OutboxMqRetryHeaders() {
    }
}
