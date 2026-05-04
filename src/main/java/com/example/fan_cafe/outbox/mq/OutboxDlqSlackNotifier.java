package com.example.fan_cafe.outbox.mq;

import com.example.fan_cafe.notification.adapter.SlackWebhookClient;
import com.example.fan_cafe.notification.domain.NotificationEvent;
import com.example.fan_cafe.notification.domain.NotificationLevel;
import com.example.fan_cafe.notification.domain.NotificationOpsType;
import com.example.fan_cafe.outbox.application.OutboxPayloadJson;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * DLQ로 넘어간 메시지는 자동 재처리 대상이 아니므로 운영자가 빠르게 인지하도록 Slack으로 알린다.
 */
@Component
@RequiredArgsConstructor
public class OutboxDlqSlackNotifier {

    private final SlackWebhookClient slackWebhookClient;
    private final OutboxPayloadJson outboxPayloadJson;

    public void notifyDlq(String payload, int retryCount, String errorMessage, DlqRoutingType routingType) {
        String eventId = outboxPayloadJson.tryExtractEventId(payload).orElse("(unknown)");
        String safeError = errorMessage != null ? errorMessage : "";

        NotificationEvent event = NotificationEvent.of(
                NotificationOpsType.OUTBOX,
                NotificationLevel.ERROR,
                "Outbox DLQ: " + routingType.name(),
                "eventId=" + eventId + ", retryCount=" + retryCount + ", routingType=" + routingType.name()
                        + "\n" + safeError,
                Map.of(
                        "eventId", eventId,
                        "retryCount", retryCount,
                        "errorMessage", safeError,
                        "routingType", routingType.name()
                )
        );
        slackWebhookClient.send(event);
    }
}
