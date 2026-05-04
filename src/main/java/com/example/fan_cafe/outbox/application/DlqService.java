package com.example.fan_cafe.outbox.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.outbox.domain.DlqEvent;
import com.example.fan_cafe.outbox.domain.DlqRoutingType;
import com.example.fan_cafe.outbox.exception.DlqErrorCode;
import com.example.fan_cafe.outbox.infrastructure.DlqEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.fan_cafe.outbox.mq.OutboxMqRetryHeaders.X_RETRY_COUNT;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_EXCHANGE;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_ROUTING_KEY;

/**
 * DLQ 메시지의 DB 스냅샷 조회와, 조건부 메인 큐 재발행을 담당한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DlqService {

    private final DlqEventRepository dlqEventRepository;
    private final OutboxPayloadJson outboxPayloadJson;
    private final RabbitTemplate rabbitTemplate;

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

        rabbitTemplate.convertAndSend(OUTBOX_EXCHANGE, OUTBOX_ROUTING_KEY, latest.getPayload(), message -> {
            message.getMessageProperties().setHeader(X_RETRY_COUNT, 0);
            return message;
        });
        log.info("[DLQ RETRY] re-published to main queue eventId={}", eventId);
    }
}
