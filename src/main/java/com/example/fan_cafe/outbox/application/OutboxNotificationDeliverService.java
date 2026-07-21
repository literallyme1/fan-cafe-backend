package com.example.fan_cafe.outbox.application;

import com.example.fan_cafe.notification.application.NotificationDispatcher;
import com.example.fan_cafe.outbox.domain.ProcessedEvent;
import com.example.fan_cafe.outbox.exception.DuplicateProcessedEventException;
import com.example.fan_cafe.outbox.infrastructure.ProcessedEventRedisCache;
import com.example.fan_cafe.outbox.infrastructure.ProcessedEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxNotificationDeliverService {

    private final NotificationDispatcher dispatcher;
    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;
    private final ProcessedEventRedisCache processedEventRedisCache;

    @Transactional
    public void deliverAndRecordProcessedEvent(String payload, String eventId, String consumerType) {
        Long receiverId = extractReceiverId(payload);
        dispatcher.dispatch(receiverId, payload);

        try {
            processedEventRepository.saveAndFlush(ProcessedEvent.record(eventId, consumerType));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateProcessedEventException(e);
        }

        registerRedisAfterCommit(eventId, consumerType);
    }

    private void registerRedisAfterCommit(String eventId, String consumerType) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    processedEventRedisCache.markProcessed(eventId, consumerType);
                    log.debug("[OUTBOX] redis marked after commit eventId={}", eventId);
                }
            });
        } else {
            processedEventRedisCache.markProcessed(eventId, consumerType);
        }
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
