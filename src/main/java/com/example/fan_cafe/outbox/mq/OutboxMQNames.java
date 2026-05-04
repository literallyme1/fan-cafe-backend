package com.example.fan_cafe.outbox.mq;

/**
 * Outbox 관련 Exchange·큐·라우팅 키 상수.
 */
public final class OutboxMQNames {
    public static final String OUTBOX_EXCHANGE = "outbox.exchange";
    public static final String OUTBOX_ROUTING_KEY = "outbox.event";
    public static final String OUTBOX_QUEUE = "outbox.queue";

    /**
     * 일시 오류 시 대기시간을 단계적으로 늘리기 위한 retry 전용 큐들이다.
     * 컨슈머를 두지 않고, 아래 TTL이 지나면 dead-letter로 다시 {@link #OUTBOX_EXCHANGE}에 흘려보내
     * 동일 {@link #OUTBOX_ROUTING_KEY}로 {@link #OUTBOX_QUEUE}에 재적재한다.
     */
    public static final String OUTBOX_RETRY_5S_QUEUE = "outbox.retry.5s";
    public static final String OUTBOX_RETRY_30S_QUEUE = "outbox.retry.30s";
    public static final String OUTBOX_RETRY_1M_QUEUE = "outbox.retry.1m";

    public static final String OUTBOX_RETRY_5S_ROUTING_KEY = "outbox.retry.5s";
    public static final String OUTBOX_RETRY_30S_ROUTING_KEY = "outbox.retry.30s";
    public static final String OUTBOX_RETRY_1M_ROUTING_KEY = "outbox.retry.1m";

    /**
     * 재처리 불가·재시도 초과 메시지를 격리 보관하는 큐로,
     * 메인 파이프라인과 분리해 데이터 유실 없이 수동 분석·복구할 수 있게 한다.
     */
    public static final String OUTBOX_DLQ_QUEUE = "outbox.dlq.queue";
    public static final String OUTBOX_DLQ_ROUTING_KEY = "outbox.dlq";

    private OutboxMQNames() {
    }
}
