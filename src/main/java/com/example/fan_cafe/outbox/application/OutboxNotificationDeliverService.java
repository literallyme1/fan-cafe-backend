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

/**
 * 실제 알림 발송과 {@code processed_events} 영속화를 같은 트랜잭션으로 묶는다.
 * Redis 쓰기는 트랜잭션 바깥이어야 하므로 {@link TransactionSynchronization#afterCommit()}에서 수행한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxNotificationDeliverService {

    private final NotificationDispatcher dispatcher;
    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;
    private final ProcessedEventRedisCache processedEventRedisCache;

    /**
     * 발송 + processed_events 기록을 한 트랜잭션으로 처리한다.
     * 커밋 성공 후에만 Redis 캐시를 채워 최종 일관성을 맞춘다(DB UNIQUE가 권위).
     */
    @Transactional
    public void deliver(String payload, String eventId, String consumerType) {
        Long receiverId = extractReceiverId(payload);
        dispatcher.dispatch(receiverId, payload);

        try {
            processedEventRepository.saveAndFlush(ProcessedEvent.record(eventId, consumerType));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateProcessedEventException(e);
        }

        // 롤백 시 Redis를 건드리지 않기 위해 커밋 이후에만 캐시 반영
        registerRedisAfterCommit(eventId, consumerType);
    }

    /** 트랜잭션 활성 시 afterCommit에만 Redis SET; 테스트 등 비트랜잭션에서는 즉시 SET. */
    private void registerRedisAfterCommit(String eventId, String consumerType) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) { //DB 저장 성공 후 redis 저장
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    processedEventRedisCache.markProcessed(eventId, consumerType);
                    log.debug("[OUTBOX] redis marked after commit eventId={}", eventId);
                }
            });
        } else { //트랜잭션 없을 시 바로 저장
            processedEventRedisCache.markProcessed(eventId, consumerType);
        }
    }

    /** userId/receiverId 미포함 시 DLQ로 분류되도록 IllegalArgumentException. */
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
