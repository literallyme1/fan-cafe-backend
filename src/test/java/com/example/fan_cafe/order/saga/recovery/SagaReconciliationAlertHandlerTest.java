package com.example.fan_cafe.order.saga.recovery;

import com.example.fan_cafe.notification.adapter.SlackWebhookClient;
import com.example.fan_cafe.notification.domain.NotificationEvent;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SagaReconciliationAlertHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final SlackWebhookClient slackWebhookClient = mock(SlackWebhookClient.class);
    private final SagaReconciliationAlertHandler handler =
            new SagaReconciliationAlertHandler(objectMapper, slackWebhookClient);

    @Test
    void reconciliationPayloadIsDeliveredToSlackOnce() throws Exception {
        UUID sagaId = UUID.randomUUID();
        String payload = objectMapper.writeValueAsString(SagaReconciliationAlert.of(
                sagaId, 10L, SagaStatus.PAYMENT_UNKNOWN, 3, "timeout"));

        assertThat(handler.handleIfSupported(payload)).isTrue();

        ArgumentCaptor<NotificationEvent> event = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(slackWebhookClient).sendOrThrow(event.capture());
        assertThat(event.getValue().getContext())
                .containsEntry("sagaId", sagaId)
                .containsEntry("retryCount", 3);
    }

    @Test
    void ordinaryOutboxPayloadIsNotConsumedAsAlert() {
        assertThat(handler.handleIfSupported("{\"eventType\":\"ORDER_PAID\"}"))
                .isFalse();
        verify(slackWebhookClient, never()).sendOrThrow(org.mockito.ArgumentMatchers.any());
    }
}
