package com.example.fan_cafe.outbox.mq;

import com.example.fan_cafe.global.test.FaultStatus;
import com.example.fan_cafe.outbox.application.OutboxMessageProcessingService;
import com.example.fan_cafe.outbox.application.OutboxMessagingExceptionRouter;
import com.example.fan_cafe.outbox.application.OutboxPayloadJson;
import com.example.fan_cafe.outbox.domain.DlqRoutingType;
import com.example.fan_cafe.outbox.exception.NonRetryableException;
import com.example.fan_cafe.outbox.exception.RetryableException;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

import static com.example.fan_cafe.outbox.mq.OutboxMqRetryHeaders.X_RETRY_COUNT;
import static com.example.fan_cafe.outbox.mq.OutboxMQNames.OUTBOX_QUEUE;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxConsumer {

    private final OutboxMessageProcessingService messageProcessingService;
    private final OutboxFailureRoutingPublisher outboxFailureRoutingPublisher;
    private final OutboxPayloadJson outboxPayloadJson;
    private final Optional<FaultStatus> faultStatus;

    @RabbitListener(queues = OUTBOX_QUEUE, ackMode = "MANUAL")
    public void consume(String payload, Message message, Channel channel) throws IOException {
        String traceId = resolveTraceIdHeader(message);
        String previousTraceId = MDC.get("traceId");
        try {
            if (traceId != null) {
                MDC.put("traceId", traceId);
            } else {
                MDC.remove("traceId");
            }
            long tag = message.getMessageProperties().getDeliveryTag();
            int currentRetry = resolveRetryCount(message);
            String eventIdForLog = outboxPayloadJson.tryExtractEventId(payload).orElse("(unknown)");
            log.info("[OUTBOX CONSUME] received eventId={}, retryCount={}", eventIdForLog, currentRetry);

            try {
                if (faultStatus.map(FaultStatus::isNotificationBlocked).orElse(false)) {
                    log.warn("[FAULT] Delivery Blocked by Admin eventId={}", eventIdForLog);
                    throw new RetryableException("FAULT_INJECTION: FINAL_DELIVERY_FAILED");
                }
                OutboxMessageProcessingService.ProcessOutcome outcome = messageProcessingService.processIdempotently(payload);
                log.info("[OUTBOX CONSUME] completed eventId={}, outcome={}", eventIdForLog, outcome);
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
        } finally {
            if (previousTraceId != null) {
                MDC.put("traceId", previousTraceId);
            } else {
                MDC.remove("traceId");
            }
        }
    }

    private static String resolveTraceIdHeader(Message message) {
        Object raw = message.getMessageProperties().getHeaders().get("traceId");
        return raw == null ? null : raw.toString();
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
                    "[OUTBOX DLQ] retry exhausted eventId={}, retryCount={}, reason={}",
                    eventIdForLog,
                    nextRetryCount,
                    e.getMessage()
            );
            outboxFailureRoutingPublisher.publishToDlq(payload, nextRetryCount, errorMessage, DlqRoutingType.RETRY_EXCEEDED);
            return;
        }
        log.warn(
                "[OUTBOX RETRY] scheduling backoff eventId={}, nextRetryCount={}, reason={}",
                eventIdForLog,
                nextRetryCount,
                e.getMessage()
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
