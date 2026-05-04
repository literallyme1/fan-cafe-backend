package com.example.fan_cafe.outbox.mq;

/** Outbox 관련 Exchange·큐·라우팅 키 상수. */
public final class OutboxMQNames {
    public static final String OUTBOX_EXCHANGE = "outbox.exchange";
    public static final String OUTBOX_ROUTING_KEY = "outbox.event";
    public static final String OUTBOX_QUEUE = "outbox.queue";

    /** Retry 메시지 적재 큐; exchange 바인딩 시 라우팅 키는 {@link #OUTBOX_RETRY_ROUTING_KEY}. */
    public static final String OUTBOX_RETRY_QUEUE = "outbox.retry.queue";
    /** DLQ; 바인딩 라우팅 키는 {@link #OUTBOX_DLQ_ROUTING_KEY}. */
    public static final String OUTBOX_DLQ_QUEUE = "outbox.dlq.queue";
    public static final String OUTBOX_RETRY_ROUTING_KEY = "outbox.retry";
    public static final String OUTBOX_DLQ_ROUTING_KEY = "outbox.dlq";

    private OutboxMQNames() {
    }
}
