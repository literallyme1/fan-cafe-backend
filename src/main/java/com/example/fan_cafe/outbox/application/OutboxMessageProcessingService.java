package com.example.fan_cafe.outbox.application;

import com.example.fan_cafe.outbox.exception.DuplicateProcessedEventException;
import com.example.fan_cafe.outbox.infrastructure.ProcessedEventRedisCache;
import com.example.fan_cafe.outbox.infrastructure.ProcessedEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * RabbitMQ Outbox 메시지 처리 진입점. Redis→DB 순으로 중복 여부를 보고,
 * 신규만 {@link OutboxNotificationDeliverService}로 넘긴다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxMessageProcessingService {

    /** processed_events·Redis 키에 붙는 소비자 구분값. */
    public static final String CONSUMER_TYPE_OUTBOX_NOTIFICATION = "OUTBOX_NOTIFICATION";

    private final ProcessedEventRedisCache processedEventRedisCache;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;
    private final OutboxNotificationDeliverService outboxNotificationDeliverService;

    public enum ProcessOutcome {
        /** 최초 처리 성공(트랜잭션 커밋까지 포함). */
        PROCESSED,
        /** 이미 처리됨(또는 동시 삽입 경합으로 중복으로 확정). */
        DUPLICATE_SKIPPED
    }

    /**
     * Read-through 순서: Redis 히트 → 즉시 스킵 / Redis 미스 → DB 존재 시 캐시 워밍 후 스킵 /
     * 둘 다 없으면 트랜잭션 내 발송+DB insert, 커밋 후 Redis는 {@link OutboxNotificationDeliverService}에서 설정.
     */
    public ProcessOutcome process(String payload) {
        String eventId = extractEventId(payload);
        String consumerType = CONSUMER_TYPE_OUTBOX_NOTIFICATION;

        // 1) Redis: 빠른 중복 판별
        if (processedEventRedisCache.isProcessed(eventId, consumerType)) {
            log.info(
                    "[OUTBOX IDEMPOTENT] redis hit, skip eventId={}, consumerType={}",
                    eventId,
                    consumerType
            );
            return ProcessOutcome.DUPLICATE_SKIPPED;
        }

        // 2) DB: 권위 있는 중복 여부. 있으면 캐시만 채우고 끝낸다.
        if (processedEventRepository.existsByEventIdAndConsumerType(eventId, consumerType)) {
            processedEventRedisCache.markProcessed(eventId, consumerType);
            log.info(
                    "[OUTBOX IDEMPOTENT] db hit, cache warmed eventId={}, consumerType={}",
                    eventId,
                    consumerType
            );
            return ProcessOutcome.DUPLICATE_SKIPPED;
        }

        try {
            // 3) 신규: 트랜잭션 안에서 발송 + processed_events insert
            outboxNotificationDeliverService.deliver(payload, eventId, consumerType);
            return ProcessOutcome.PROCESSED;
        } catch (DuplicateProcessedEventException e) {
            // 동시에 다른 워커가 먼저 커밋한 경우 — 중복으로 간주하고 캐시만 맞춘다.
            processedEventRedisCache.markProcessed(eventId, consumerType);
            log.info(
                    "[OUTBOX IDEMPOTENT] unique constraint race, treat as done eventId={}",
                    eventId
            );
            return ProcessOutcome.DUPLICATE_SKIPPED;
        }
    }

    /** 페이로드 JSON의 eventId(outbox PK 문자열). 없으면 DLQ로 보내기 위해 IllegalArgumentException. */
    private String extractEventId(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            if (node.hasNonNull(OutboxPayloadJson.EVENT_ID_FIELD)) {
                return node.get(OutboxPayloadJson.EVENT_ID_FIELD).asText();
            }
        } catch (Exception ignored) {
        }
        throw new IllegalArgumentException("eventId not found in outbox message payload");
    }
}
