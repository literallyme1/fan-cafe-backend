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
        List<OutboxEvent> events = outboxEventRepository.findProcessableEventsForUpdate(now);

        for (OutboxEvent event : events) {
            log.info("[RELAY] - PENDING 레코드 감지 (ID: {}) -> 발송 시도", event.getId());
            if (event.getRetryCount() > 0) {
                log.info("[RELAY] - 재시도 시작 (Attempt: {}/{})",
                        event.getRetryCount(), OutboxEvent.MAX_RETRY_COUNT);
            }

            OutboxEventStatus statusBefore = event.getStatus();
            try {
                String eventKey = event.getEventId() != null ? event.getEventId() : String.valueOf(event.getId());
                String payloadToPublish = outboxPayloadJson.mergeEventId(event.getPayload(), eventKey);
                outboxPublisher.publish(payloadToPublish);
                event.markSent();
                log.info("[SOCKET] - Expo 클라이언트로 알림 전송 성공!");
                log.info("[OUTBOX] - 상태 변경 완료: {} -> {}", statusBefore, event.getStatus());
            } catch (Exception e) {
                int previousRetryCount = event.getRetryCount();
                String errorTag = classifyErrorTag(e);
                if (previousRetryCount == 0) {
                    if (TAG_TIMEOUT.equals(errorTag)) {
                        log.error("[RELAY] - 발송 실패: NOTIFICATION_RELAY_TIMEOUT (Fault Injection Active)", e);
                    } else {
                        log.error("[RELAY] - 발송 실패: {} ({})", errorTag, safeMessage(e), e);
                    }
                    log.warn("[SLACK] - [긴급] 알림 발송 실패 보고 (Retry 예정: {}회차)", previousRetryCount + 1);
                } else {
                    log.error("[RELAY] - 발송 실패: 통신 장애 지속 중", e);
                }

                event.fail(
                        formatLastError(errorTag, safeMessage(e)),
                        outboxRetryPolicy.nextRetry(event.getRetryCount() + 1)
                );
                if (event.isManualRequired()) {
                    NotificationEvent notificationEvent = NotificationEvent.of(
                            NotificationOpsType.OUTBOX,
                            NotificationLevel.ERROR,
                            "Outbox retry 초과",
                            "Outbox eventId=" + event.getId() + " 가 수동 조치 상태로 전이되었습니다.",
                            Map.of(
                                    "retryCount", event.getRetryCount(),
                                    "error", event.getLastError()
                            )
                    );
                    slackWebhookClient.send(notificationEvent);
                }
                log.warn("[OUTBOX PUBLISH FAIL] id={}, code={}, retryCount={}",
                        event.getId(), errorTag, event.getRetryCount(), e);
            }
        }
    }

    private String classifyErrorTag(Throwable throwable) {
        if (containsType(throwable, AmqpTimeoutException.class) || containsType(throwable, SocketTimeoutException.class)) {
            return TAG_TIMEOUT;
        }
        if (containsType(throwable, AmqpConnectException.class)) {
            return TAG_CONNECTION;
        }
        if (containsType(throwable, MessageConversionException.class) || containsType(throwable, JsonProcessingException.class)) {
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
