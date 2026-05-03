package com.example.fan_cafe.outbox.application;

import com.example.fan_cafe.notification.application.NotificationDispatcher;
import com.example.fan_cafe.outbox.domain.ProcessedEvent;
import com.example.fan_cafe.outbox.infrastructure.ProcessedEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxMessageProcessingService {

    public static final String CONSUMER_TYPE_OUTBOX_NOTIFICATION = "OUTBOX_NOTIFICATION";

    private final NotificationDispatcher dispatcher;
    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;

    public enum ProcessOutcome {
        PROCESSED,
        DUPLICATE_SKIPPED
    }

    @Transactional
    public ProcessOutcome process(String payload) {
        String eventId = extractEventId(payload);
        if (processedEventRepository.existsByEventIdAndConsumerType(eventId, CONSUMER_TYPE_OUTBOX_NOTIFICATION)) {
            log.info(
                    "[OUTBOX IDEMPOTENT] already recorded, skip processing eventId={}, consumerType={}",
                    eventId,
                    CONSUMER_TYPE_OUTBOX_NOTIFICATION
            );
            return ProcessOutcome.DUPLICATE_SKIPPED;
        }

        Long receiverId = extractReceiverId(payload);
        dispatcher.dispatch(receiverId, payload);

        try {
            processedEventRepository.saveAndFlush(ProcessedEvent.record(eventId, CONSUMER_TYPE_OUTBOX_NOTIFICATION));
        } catch (DataIntegrityViolationException e) {
            log.info(
                    "[OUTBOX IDEMPOTENT] unique constraint (concurrent or duplicate delivery), eventId={}",
                    eventId,
                    e
            );
            return ProcessOutcome.DUPLICATE_SKIPPED;
        }
        return ProcessOutcome.PROCESSED;
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

    private Long extractReceiverId(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            if (node.hasNonNull("receiverId")) {
                return node.get("receiverId").asLong();
            }
            if (node.hasNonNull("userId")) {
                return node.get("userId").asLong();
            }
        } catch (Exception ignored) {
        }
        throw new IllegalArgumentException("Receiver id not found in payload");
    }
}
