package com.example.fan_cafe.outbox.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.outbox.domain.DlqEvent;
import com.example.fan_cafe.outbox.domain.DlqRoutingType;
import com.example.fan_cafe.outbox.exception.DlqErrorCode;
import com.example.fan_cafe.outbox.infrastructure.DlqEventRepository;
import com.example.fan_cafe.outbox.infrastructure.ProcessedEventRedisCache;
import com.example.fan_cafe.outbox.infrastructure.ProcessedEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpTimeoutException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static com.example.fan_cafe.outbox.mq.OutboxMqRetryHeaders.X_RETRY_COUNT;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_EXCHANGE;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_ROUTING_KEY;

/**
 * DLQ 메시지의 DB 스냅샷 조회와, 조건부 메인 큐 재발행을 담당한다.
 */
@Service
@Slf4j
public class DlqService {

    private static final String OUTBOX_CONSUMER_TYPE = "OUTBOX_NOTIFICATION";

    private final DlqEventRepository dlqEventRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ProcessedEventRedisCache processedEventRedisCache;
    private final OutboxPayloadJson outboxPayloadJson;
    private final RabbitTemplate rabbitTemplate;
    private final long publisherConfirmTimeoutMs;

    public DlqService(
            DlqEventRepository dlqEventRepository,
            ProcessedEventRepository processedEventRepository,
            ProcessedEventRedisCache processedEventRedisCache,
            OutboxPayloadJson outboxPayloadJson,
            RabbitTemplate rabbitTemplate,
            @Value("${outbox.publisher.confirm-timeout-ms:5000}") long publisherConfirmTimeoutMs
    ) {
        this.dlqEventRepository = dlqEventRepository;
        this.processedEventRepository = processedEventRepository;
        this.processedEventRedisCache = processedEventRedisCache;
        this.outboxPayloadJson = outboxPayloadJson;
        this.rabbitTemplate = rabbitTemplate;
        this.publisherConfirmTimeoutMs = publisherConfirmTimeoutMs;
    }

    @Transactional(readOnly = true)
    public List<DlqEvent> findAllOrderByNewest() {
        return dlqEventRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public DlqEvent getLatestByEventId(String eventId) {
        return dlqEventRepository.findTopByEventIdOrderByCreatedAtDesc(eventId)
                .orElseThrow(() -> new CustomException(DlqErrorCode.DLQ_EVENT_NOT_FOUND));
    }

    /**
     * 브로커 DLQ 적재와 동일한 시점에 호출되어 운영 화면에서 이력을 조회할 수 있게 한다.
     */
    @Transactional
    public void persistFromMqPublish(
            String payload,
            int retryCount,
            String errorMessage,
            DlqRoutingType routingType
    ) {
        String eventId = outboxPayloadJson.tryExtractEventId(payload).orElse("unknown");
        DlqEvent row = DlqEvent.create(eventId, payload, errorMessage, retryCount, routingType);
        dlqEventRepository.save(row);
    }

    /**
     * 수동 재처리: 재시도 소진으로만 들어온 건만 메인 라우팅으로 되돌린다.
     */
    @Transactional
    public void retryToMainQueue(String eventId) {
        DlqEvent latest = dlqEventRepository.findTopByEventIdOrderByCreatedAtDesc(eventId)
                .orElseThrow(() -> new CustomException(DlqErrorCode.DLQ_EVENT_NOT_FOUND));

        if (latest.getRoutingType() != DlqRoutingType.RETRY_EXCEEDED) {
            throw new CustomException(DlqErrorCode.DLQ_NOT_RETRYABLE);
        }

        // 수동 DLQ 재처리는 "동일 eventId 재실행" 의도이므로 idempotency 흔적을 비운다.
        String payload = latest.getPayload();
        String traceId = MDC.get("traceId");
        long deletedRows = processedEventRepository.deleteByEventIdAndConsumerType(eventId, OUTBOX_CONSUMER_TYPE);
        processedEventRedisCache.clearProcessed(eventId, OUTBOX_CONSUMER_TYPE);

        // DB 커밋 전에는 idempotency 체크(있음 조회)를 할 수 있으니, 커밋 이후에만 재발행한다.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishToMainQueueWithConfirm(eventId, payload, traceId, deletedRows);
                }
            });
            return;
        }

        // 동기화가 활성화되지 않았다면(테스트/특수상황) 즉시 재발행
        publishToMainQueueWithConfirm(eventId, payload, traceId, deletedRows);
    }

    private void publishToMainQueueWithConfirm(String eventId, String payload, String traceId, long deletedRows) {
        boolean confirmed = Boolean.TRUE.equals(rabbitTemplate.invoke(ops -> {
            ops.convertAndSend(OUTBOX_EXCHANGE, OUTBOX_ROUTING_KEY, payload, message -> {
                message.getMessageProperties().setHeader(X_RETRY_COUNT, 0);
                message.getMessageProperties().setHeader("traceId", traceId);
                return message;
            });
            return ops.waitForConfirms(publisherConfirmTimeoutMs);
        }));
        if (!confirmed) {
            throw new AmqpTimeoutException(
                    "DLQ retry publish confirm timed out after " + publisherConfirmTimeoutMs + " ms"
            );
        }
        log.info("[DLQ RETRY] re-published to main queue eventId={}, deletedProcessedRows={}", eventId, deletedRows);
    }
}
