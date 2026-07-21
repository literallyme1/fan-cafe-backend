package com.example.fan_cafe.outbox.application;

import com.example.fan_cafe.outbox.infrastructure.ProcessedEventRedisCache;
import com.example.fan_cafe.outbox.infrastructure.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxMessageProcessingServiceTest {

    private static final String PAYLOAD =
            "{\"eventId\":\"42\",\"eventType\":\"ORDER_PAID\",\"userId\":1}";

    @Mock
    private ProcessedEventRedisCache processedEventRedisCache;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private OutboxNotificationDeliverService outboxNotificationDeliverService;

    @InjectMocks
    private OutboxMessageProcessingService outboxMessageProcessingService;

    @BeforeEach
    void setUp() {
        when(processedEventRepository.existsByEventIdAndConsumerType(
                "42", OutboxMessageProcessingService.CONSUMER_TYPE_OUTBOX_NOTIFICATION))
                .thenReturn(false);
    }

    @Test
    @DisplayName("Redis isProcessed 장애 시 DB fallback으로 신규 처리를 이어간다.")
    void process_shouldFallbackToDb_whenRedisCheckFails() {
        doThrow(new RuntimeException("redis timeout"))
                .when(processedEventRedisCache)
                .isProcessed("42", OutboxMessageProcessingService.CONSUMER_TYPE_OUTBOX_NOTIFICATION);

        OutboxMessageProcessingService.ProcessOutcome outcome =
                outboxMessageProcessingService.processIdempotently(PAYLOAD);

        assertThat(outcome).isEqualTo(OutboxMessageProcessingService.ProcessOutcome.PROCESSED);
        verify(processedEventRepository).existsByEventIdAndConsumerType(
                "42", OutboxMessageProcessingService.CONSUMER_TYPE_OUTBOX_NOTIFICATION);
        verify(outboxNotificationDeliverService).deliverAndRecordProcessedEvent(
                eq(PAYLOAD),
                eq("42"),
                eq(OutboxMessageProcessingService.CONSUMER_TYPE_OUTBOX_NOTIFICATION));
    }

    @Test
    @DisplayName("Redis 장애 후 DB에 이미 처리 이력이 있으면 DUPLICATE_SKIPPED 한다.")
    void process_shouldSkipDuplicate_whenRedisFailsAndDbHit() {
        doThrow(new RuntimeException("redis connection refused"))
                .when(processedEventRedisCache)
                .isProcessed(anyString(), anyString());
        when(processedEventRepository.existsByEventIdAndConsumerType(
                "42", OutboxMessageProcessingService.CONSUMER_TYPE_OUTBOX_NOTIFICATION))
                .thenReturn(true);

        OutboxMessageProcessingService.ProcessOutcome outcome =
                outboxMessageProcessingService.processIdempotently(PAYLOAD);

        assertThat(outcome).isEqualTo(OutboxMessageProcessingService.ProcessOutcome.DUPLICATE_SKIPPED);
        verify(outboxNotificationDeliverService, never()).deliverAndRecordProcessedEvent(anyString(), anyString(), anyString());
    }
}
