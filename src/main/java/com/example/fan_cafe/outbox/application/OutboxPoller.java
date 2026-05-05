package com.example.fan_cafe.outbox.application;

import com.example.fan_cafe.outbox.application.retry.OutboxRetryPolicy;
import com.example.fan_cafe.outbox.domain.OutboxEvent;
import com.example.fan_cafe.outbox.domain.OutboxEventStatus;
import com.example.fan_cafe.outbox.infrastructure.OutboxEventRepository;
import com.example.fan_cafe.notification.adapter.SlackWebhookClient;
import com.example.fan_cafe.notification.domain.NotificationEvent;
import com.example.fan_cafe.notification.domain.NotificationLevel;
import com.example.fan_cafe.notification.domain.NotificationOpsType;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.AmqpTimeoutException;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private static final String TAG_TIMEOUT = "MQ_TIMEOUT";
    private static final String TAG_CONNECTION = "MQ_CONNECTION_ERROR";
    private static final String TAG_SERIALIZATION = "MQ_SERIALIZATION_ERROR";
    private static final String TAG_UNKNOWN = "MQ_UNKNOWN_ERROR";

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxPublisher outboxPublisher;
    private final OutboxRetryPolicy outboxRetryPolicy;
    private final SlackWebhookClient slackWebhookClient;
    private final OutboxPayloadJson outboxPayloadJson;

    private volatile LocalDateTime lastExecutedAt;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void poll() {

        LocalDateTime now = LocalDateTime.now();
        lastExecutedAt = now;

        List<OutboxEvent> events =
                outboxEventRepository.findProcessableEventsForUpdate(now);

        log.info("[OutboxPoller] [LOCK-START] FOR UPDATE SKIP LOCKED executed, batchSize={}", events.size());

        for (OutboxEvent event : events) {

            log.info("[OutboxPoller] [LOCK-ACQUIRED] eventId={} lock acquired (processing started)",
                    event.getId());

            OutboxEventStatus statusBefore = event.getStatus();

            try {
                // PENDING 감지
                log.info("[OUTBOX-POLL] eventId={} status=PENDING detected, publishing started",
                        event.getId());

                if (event.getRetryCount() > 0) {
                    log.info("[OUTBOX-RETRY] eventId={} attempt={}/{}",
                            event.getId(),
                            event.getRetryCount(),
                            OutboxEvent.MAX_RETRY_COUNT);
                }

                //publish
                String eventKey = event.getEventId() != null
                        ? event.getEventId()
                        : String.valueOf(event.getId());

                String payloadToPublish =
                        outboxPayloadJson.mergeEventId(event.getPayload(), eventKey);

                outboxPublisher.publish(payloadToPublish);

                // 성공
                event.markSent();

                log.info("[OUTBOX-PUBLISH] eventId={} publish success", event.getId());

                log.info("[OUTBOX-STATUS] eventId={} status {} -> {}",
                        event.getId(), statusBefore, event.getStatus());

                log.info("[NOTIFICATION] push delivered to client");

            } catch (Exception e) {

                int previousRetryCount = event.getRetryCount();
                String errorTag = classifyErrorTag(e);

                // 첫 실패
                if (previousRetryCount == 0) {

                    log.error("[OUTBOX-ERROR] eventId={} code={} message={}",
                            event.getId(), errorTag, safeMessage(e));

                    log.warn("[ALERT] Slack notification triggered for eventId={}, nextRetry={}",
                            event.getId(), previousRetryCount + 1);

                } else {
                    // 지속 실패
                    log.error("[OUTBOX-ERROR] eventId={} retrying due to persistent failure",
                            event.getId());
                }

                // 상태 변경
                event.fail(
                        formatLastError(errorTag, safeMessage(e)),
                        outboxRetryPolicy.nextRetry(event.getRetryCount() + 1)
                );

                // retry 초과 → 수동 처리
                if (event.isManualRequired()) {

                    log.error("[OUTBOX-DLQ] eventId={} retryExceeded, manual intervention required",
                            event.getId());

                    NotificationEvent notificationEvent = NotificationEvent.of(
                            NotificationOpsType.OUTBOX,
                            NotificationLevel.ERROR,
                            "Outbox retry exceeded",
                            "Outbox eventId=" + event.getId() + " requires manual intervention.",
                            Map.of(
                                    "retryCount", event.getRetryCount(),
                                    "error", event.getLastError()
                            )
                    );

                    slackWebhookClient.send(notificationEvent);
                }

                log.warn("[OUTBOX-FAIL] eventId={} code={} retryCount={}",
                        event.getId(), errorTag, event.getRetryCount());
            } finally {
                // LOCK RELEASE
                log.info("[OutboxPoller] [LOCK-RELEASE] eventId={} processing completed, lock released",
                        event.getId());
            }
        }
    }

    private String classifyErrorTag(Throwable throwable) {
        if (containsType(throwable, AmqpTimeoutException.class)
                || containsType(throwable, SocketTimeoutException.class)) {
            return TAG_TIMEOUT;
        }
        if (containsType(throwable, AmqpConnectException.class)) {
            return TAG_CONNECTION;
        }
        if (containsType(throwable, MessageConversionException.class)
                || containsType(throwable, JsonProcessingException.class)) {
            return TAG_SERIALIZATION;
        }
        if (containsType(throwable, AmqpException.class)) {
            return TAG_CONNECTION;
        }
        return TAG_UNKNOWN;
    }

    private boolean containsType(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String safeMessage(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null) {
            return "unknown publish error";
        }
        return throwable.getMessage();
    }

    private String formatLastError(String errorTag, String errorMessage) {
        return "[" + errorTag + "] " + errorMessage;
    }

    public LocalDateTime getLastExecutedAt() {
        return lastExecutedAt;
    }
}