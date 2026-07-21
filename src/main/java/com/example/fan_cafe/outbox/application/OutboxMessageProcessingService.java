package com.example.fan_cafe.outbox.application;

import com.example.fan_cafe.outbox.exception.DuplicateProcessedEventException;
import com.example.fan_cafe.outbox.infrastructure.ProcessedEventRedisCache;
import com.example.fan_cafe.outbox.infrastructure.ProcessedEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxMessageProcessingService {

    public static final String CONSUMER_TYPE_OUTBOX_NOTIFICATION = "OUTBOX_NOTIFICATION";

    private final ProcessedEventRedisCache processedEventRedisCache;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;
    private final OutboxNotificationDeliverService outboxNotificationDeliverService;

    public enum ProcessOutcome {
        PROCESSED,
        DUPLICATE_SKIPPED
    }

    public ProcessOutcome processIdempotently(String payload) {
        String eventId = extractEventId(payload);
        String consumerType = CONSUMER_TYPE_OUTBOX_NOTIFICATION;

        if (isProcessedInRedis(eventId, consumerType)) {
            log.info(
                    "[OUTBOX IDEMPOTENT] redis hit, skip eventId={}, consumerType={}",
                    eventId,
                    consumerType
            );
            return ProcessOutcome.DUPLICATE_SKIPPED;
        }

        if (processedEventRepository.existsByEventIdAndConsumerType(eventId, consumerType)) {
            warmRedisCache(eventId, consumerType);
            log.info(
                    "[OUTBOX IDEMPOTENT] db hit, cache warmed eventId={}, consumerType={}",
                    eventId,
                    consumerType
            );
            return ProcessOutcome.DUPLICATE_SKIPPED;
        }

        try {
            outboxNotificationDeliverService.deliverAndRecordProcessedEvent(payload, eventId, consumerType);
            return ProcessOutcome.PROCESSED;
        } catch (DuplicateProcessedEventException e) {
            warmRedisCache(eventId, consumerType);
            log.info(
                    "[OUTBOX IDEMPOTENT] unique constraint race, treat as done eventId={}",
                    eventId
            );
            return ProcessOutcome.DUPLICATE_SKIPPED;
        }
    }

    private boolean isProcessedInRedis(String eventId, String consumerType) {
        try {
            return processedEventRedisCache.isProcessed(eventId, consumerType);
        } catch (Exception e) {
            log.warn(
                    "[OUTBOX IDEMPOTENT] redis check failed, fallback to DB eventId={}, consumerType={}",
                    eventId,
                    consumerType,
                    e
            );
            return false;
        }
    }

    private void warmRedisCache(String eventId, String consumerType) {
        try {
            processedEventRedisCache.markProcessed(eventId, consumerType);
        } catch (Exception e) {
            log.warn(
                    "[OUTBOX IDEMPOTENT] redis cache warm failed eventId={}, consumerType={}",
                    eventId,
                    consumerType,
                    e
            );
        }
    }

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
