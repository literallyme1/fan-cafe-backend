package com.example.fan_cafe.outbox;

import com.example.fan_cafe.notification.adapter.SlackWebhookClient;
import com.example.fan_cafe.outbox.application.OutboxPayloadJson;
import com.example.fan_cafe.outbox.application.OutboxPoller;
import com.example.fan_cafe.outbox.application.OutboxPublisher;
import com.example.fan_cafe.outbox.application.retry.OutboxRetryPolicy;
import com.example.fan_cafe.outbox.domain.OutboxEvent;
import com.example.fan_cafe.outbox.domain.OutboxEventStatus;
import com.example.fan_cafe.outbox.infrastructure.OutboxEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPollerTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OutboxPublisher outboxPublisher;

    @Mock
    private OutboxRetryPolicy outboxRetryPolicy;

    @Mock
    private SlackWebhookClient slackWebhookClient;

    @Mock
    private OutboxPayloadJson outboxPayloadJson;

    @InjectMocks
    private OutboxPoller outboxPoller;

    @Test
    @DisplayName("publisher 성공 시 상태가 SENT로 전이된다.")
    void poll_shouldMarkSent_whenPublishSucceeds() {
        OutboxEvent event = OutboxEvent.init("ORDER", 1L, "{\"eventType\":\"ORDER_PAID\"}");
        ReflectionTestUtils.setField(event, "id", 1L);
        ReflectionTestUtils.setField(event, "eventId", "1");
        when(outboxPayloadJson.mergeEventId(anyString(), anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxEventRepository.findProcessableEventsForUpdate(any(LocalDateTime.class))).thenReturn(List.of(event));

        outboxPoller.poll();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.SENT);
        assertThat(event.getLastError()).isNull();
    }

    @Test
    @DisplayName("publisher 실패 시 상태가 FAILED로 전이되고 retry_count가 증가한다.")
    void poll_shouldMarkFailedAndIncreaseRetryCount_whenPublishFails() {
        OutboxEvent event = OutboxEvent.init("ORDER", 2L, "{\"eventType\":\"ORDER_PAID\"}");
        ReflectionTestUtils.setField(event, "id", 2L);
        ReflectionTestUtils.setField(event, "eventId", "2");
        LocalDateTime nextRetryAt = LocalDateTime.now().plusSeconds(10);

        when(outboxPayloadJson.mergeEventId(anyString(), anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxEventRepository.findProcessableEventsForUpdate(any(LocalDateTime.class))).thenReturn(List.of(event));
        doThrow(new RuntimeException("mq publish failed")).when(outboxPublisher).publish(anyString(), nullable(String.class));
        when(outboxRetryPolicy.nextRetry(1)).thenReturn(nextRetryAt);

        outboxPoller.poll();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getNextRetryAt()).isEqualTo(nextRetryAt);
        assertThat(event.getLastError()).startsWith("[MQ_UNKNOWN_ERROR]");
    }

    @Test
    @DisplayName("max retry 초과 시 상태가 MANUAL_REQUIRED로 전이된다.")
    void poll_shouldMarkManualRequired_whenRetryExceeded() {
        OutboxEvent event = OutboxEvent.init("ORDER", 3L, "{\"eventType\":\"ORDER_CANCELLED\"}");
        ReflectionTestUtils.setField(event, "id", 3L);
        ReflectionTestUtils.setField(event, "eventId", "3");
        ReflectionTestUtils.setField(event, "retryCount", OutboxEvent.MAX_RETRY_COUNT);
        ReflectionTestUtils.setField(event, "status", OutboxEventStatus.FAILED);

        when(outboxPayloadJson.mergeEventId(anyString(), anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxEventRepository.findProcessableEventsForUpdate(any(LocalDateTime.class))).thenReturn(List.of(event));
        doThrow(new RuntimeException("still failing")).when(outboxPublisher).publish(anyString(), nullable(String.class));
        when(outboxRetryPolicy.nextRetry(OutboxEvent.MAX_RETRY_COUNT + 1)).thenReturn(LocalDateTime.now().plusMinutes(1));

        outboxPoller.poll();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.MANUAL_REQUIRED);
        assertThat(event.getRetryCount()).isEqualTo(OutboxEvent.MAX_RETRY_COUNT + 1);
        assertThat(event.getNextRetryAt()).isNull();
        assertThat(event.getLastError()).startsWith("[MQ_UNKNOWN_ERROR]");
        verify(outboxRetryPolicy).nextRetry(OutboxEvent.MAX_RETRY_COUNT + 1);
    }
}
