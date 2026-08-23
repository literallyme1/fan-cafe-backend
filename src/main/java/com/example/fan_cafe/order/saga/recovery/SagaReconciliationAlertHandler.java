package com.example.fan_cafe.order.saga.recovery;

import com.example.fan_cafe.notification.adapter.SlackWebhookClient;
import com.example.fan_cafe.notification.domain.NotificationEvent;
import com.example.fan_cafe.notification.domain.NotificationLevel;
import com.example.fan_cafe.notification.domain.NotificationOpsType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SagaReconciliationAlertHandler {

    private final ObjectMapper objectMapper;
    private final SlackWebhookClient slackWebhookClient;

    public boolean handleIfSupported(String payload) {
        SagaReconciliationAlert alert = parseIfSupported(payload);
        if (alert == null) {
            return false;
        }

        slackWebhookClient.sendOrThrow(NotificationEvent.of(
                NotificationOpsType.SYSTEM,
                NotificationLevel.ERROR,
                "Saga manual reconciliation required",
                "Automatic saga recovery reached its retry limit.",
                Map.of(
                        "sagaId", alert.sagaId(),
                        "orderId", alert.orderId(),
                        "previousStatus", alert.previousStatus(),
                        "retryCount", alert.retryCount(),
                        "lastError", alert.lastError()
                )
        ));
        return true;
    }

    private SagaReconciliationAlert parseIfSupported(String payload) {
        try {
            String eventType = objectMapper.readTree(payload).path("eventType").asText();
            if (!SagaReconciliationAlert.EVENT_TYPE.equals(eventType)) {
                return null;
            }
            return objectMapper.readValue(payload, SagaReconciliationAlert.class);
        } catch (JsonProcessingException invalidPayload) {
            throw new IllegalArgumentException("Invalid reconciliation alert payload", invalidPayload);
        }
    }
}
