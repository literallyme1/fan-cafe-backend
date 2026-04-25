package com.example.fan_cafe.outbox.mq;

public final class OutboxMQNames {
    public static final String OUTBOX_EXCHANGE = "outbox.exchange";
    public static final String OUTBOX_ROUTING_KEY = "outbox.event";
    public static final String OUTBOX_QUEUE = "outbox.queue";

    private OutboxMQNames() {
    }
}
