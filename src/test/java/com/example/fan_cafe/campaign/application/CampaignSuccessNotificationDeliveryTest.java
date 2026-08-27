package com.example.fan_cafe.campaign.application;

import com.example.fan_cafe.notification.application.NotificationDispatcher;
import com.example.fan_cafe.order.saga.recovery.SagaReconciliationAlertHandler;
import com.example.fan_cafe.outbox.application.OutboxNotificationDeliverService;
import com.example.fan_cafe.outbox.domain.ProcessedEvent;
import com.example.fan_cafe.outbox.infrastructure.ProcessedEventRedisCache;
import com.example.fan_cafe.outbox.infrastructure.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CampaignSuccessNotificationDeliveryTest {

    @Test
    void campaignSucceededPayloadUsesExistingOutboxNotificationConsumerBoundary() {
        NotificationDispatcher dispatcher = mock(NotificationDispatcher.class);
        SagaReconciliationAlertHandler alertHandler = mock(SagaReconciliationAlertHandler.class);
        ProcessedEventRepository processedRepository = mock(ProcessedEventRepository.class);
        ProcessedEventRedisCache redisCache = mock(ProcessedEventRedisCache.class);
        OutboxNotificationDeliverService service = new OutboxNotificationDeliverService(
                dispatcher, alertHandler, new ObjectMapper(), processedRepository, redisCache);
        String payload = """
                {"eventId":"campaign-42","eventType":"CAMPAIGN_SUCCEEDED",
                "campaignId":7,"receiverId":99,"message":"Campaign succeeded"}
                """;
        when(alertHandler.handleIfSupported(payload)).thenReturn(false);

        service.deliverAndRecordProcessedEvent(payload, "campaign-42", "OUTBOX_NOTIFICATION");

        verify(dispatcher).dispatch(99L, payload);
        verify(processedRepository).saveAndFlush(
                org.mockito.ArgumentMatchers.any(ProcessedEvent.class));
        verify(redisCache).markProcessed("campaign-42", "OUTBOX_NOTIFICATION");
    }
}
