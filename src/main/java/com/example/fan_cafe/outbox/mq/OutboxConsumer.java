package com.example.fan_cafe.outbox.mq;

import com.example.fan_cafe.outbox.application.OutboxMessageProcessingService;
import com.example.fan_cafe.outbox.application.OutboxMessagingExceptionRouter;
import com.example.fan_cafe.outbox.application.OutboxPayloadJson;
import com.example.fan_cafe.outbox.exception.NonRetryableException;
import com.example.fan_cafe.outbox.exception.RetryableException;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.example.fan_cafe.outbox.mq.OutboxMqRetryHeaders.X_RETRY_COUNT;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_QUEUE;

/**
 * Outbox 메인 큐 소비. 처리 성공 시에만 ACK하고,
 * 일시 오류는 단계별 retry 큐·구조적 오류는 DLQ로 넘긴 뒤 원 메시지는 ACK하여 브로커 재전달 루프를 끊는다.
 *
 * <p>{@link OutboxMqRetryHeaders#X_RETRY_COUNT}가 커질수록 지연 큐가 길어지고,
 * 상한을 넘기면 DLQ로만 빠져 동일 이벤트가 영구히 순환하지 않는다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxConsumer {

    private final OutboxMessageProcessingService messageProcessingService;
    private final OutboxFailureRoutingPublisher outboxFailureRoutingPublisher;
    private final OutboxPayloadJson outboxPayloadJson;

    @RabbitListener(queues = OUTBOX_QUEUE, ackMode = "MANUAL")
    public void consume(String payload, Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        int currentRetry = resolveRetryCount(message);
        String eventIdForLog = outboxPayloadJson.tryExtractEventId(payload).orElse("(unknown)");

        try {
            messageProcessingService.process(payload);
            channel.basicAck(tag, false);
        } catch (RetryableException e) {
            routeRetryable(payload, currentRetry, eventIdForLog, e.getMessage(), e);
            channel.basicAck(tag, false);
        } catch (NonRetryableException e) {
            routeNonRetryable(payload, currentRetry, eventIdForLog, e.getMessage(), e);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            RuntimeException routed = OutboxMessagingExceptionRouter.wrapForRouting(e);
            if (routed instanceof RetryableException re) {
                routeRetryable(payload, currentRetry, eventIdForLog, re.getMessage(), re);
            } else {
                NonRetryableException ne = (NonRetryableException) routed;
                routeNonRetryable(payload, currentRetry, eventIdForLog, ne.getMessage(), ne);
            }
            channel.basicAck(tag, false);
        }
    }

    private void routeRetryable(
            String payload,
            int currentRetry,
            String eventIdForLog,
            String errorMessage,
            Exception e
    ) {
        int nextRetryCount = currentRetry + 1;
        if (currentRetry >= 3) {
            log.error(
                    "[OUTBOX DLQ] retry exhausted eventId={}, retryCount={}, err={}",
                    eventIdForLog,
                    nextRetryCount,
                    errorMessage,
                    e
            );
            outboxFailureRoutingPublisher.publishToDlq(payload, nextRetryCount, errorMessage, DlqRoutingType.RETRY_EXCEEDED);
            return;
        }
        log.warn(
                "[OUTBOX RETRY] scheduling backoff eventId={}, nextRetryCount={}, err={}",
                eventIdForLog,
                nextRetryCount,
                errorMessage,
                e
        );
        outboxFailureRoutingPublisher.publishToRetryQueue(payload, nextRetryCount);
    }

    private void routeNonRetryable(
            String payload,
            int currentRetry,
            String eventIdForLog,
            String errorMessage,
            Exception e
    ) {
        log.error(
                "[OUTBOX DLQ] non-retryable eventId={}, retryCount={}, err={}",
                eventIdForLog,
                currentRetry,
                errorMessage,
                e
        );
        outboxFailureRoutingPublisher.publishToDlq(payload, currentRetry, errorMessage, DlqRoutingType.NON_RETRYABLE);
    }

    /**
     * 브로커가 숫자 헤더를 Integer/Long 등으로 넘길 수 있어 타입을 흡수한다. 없거나 깨지면 0(최초 소비)이다.
     */
    private static int resolveRetryCount(Message message) {
        Object raw = message.getMessageProperties().getHeaders().get(X_RETRY_COUNT);
        if (raw == null) {
            return 0;
        }
        if (raw instanceof Number n) {
            return Math.max(0, n.intValue());
        }
        try {
            return Math.max(0, Integer.parseInt(raw.toString()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
