package com.example.fan_cafe.outbox;

import com.example.fan_cafe.outbox.application.OutboxPoller;
import com.example.fan_cafe.outbox.application.OutboxPublisher;
import com.example.fan_cafe.outbox.application.retry.OutboxRetryPolicy;
import com.example.fan_cafe.outbox.domain.OutboxEvent;
import com.example.fan_cafe.outbox.domain.OutboxEventStatus;
import com.example.fan_cafe.outbox.infrastructure.OutboxEventRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Outbox Poller가 MQ 발행 실패 시 DB 상태를 올바르게 기록하는지 검증한다.
 * RabbitMQ를 중지하지 않고 {@link OutboxPublisher}를 Mock 처리한다.
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("ci")
class OutboxPublishFailureIntegrationTest {

    @Autowired
    private OutboxPoller outboxPoller;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private EntityManager entityManager;

    @MockBean
    private OutboxPublisher outboxPublisher;

    @MockBean
    private OutboxRetryPolicy outboxRetryPolicy;

    private final List<Long> createdOutboxIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        createdOutboxIds.forEach(outboxEventRepository::deleteById);
        createdOutboxIds.clear();
    }

    @Test
    @Transactional
    @DisplayName("[4차] outboxPoller_marksFailedWithLastErrorAndNextRetryAt_whenPublishFails")
    void outboxPoller_marksFailedWithLastErrorAndNextRetryAt_whenPublishFails() {
        LocalDateTime expectedNextRetry = LocalDateTime.now().plusMinutes(5);
        when(outboxRetryPolicy.nextRetry(1)).thenReturn(expectedNextRetry);
        doThrow(new RuntimeException("mq publish failed"))
                .when(outboxPublisher).publish(any(String.class), nullable(String.class));

        OutboxEvent event = persistProcessableOutbox("{\"eventType\":\"ORDER_CREATED\",\"orderId\":1}");

        outboxPoller.poll();
        flushAndClear();

        OutboxEvent reloaded = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(reloaded.getRetryCount()).isEqualTo(1);
        assertThat(reloaded.getLastError()).startsWith("[MQ_UNKNOWN_ERROR]");
        assertThat(reloaded.getLastError()).contains("mq publish failed");
        assertThat(reloaded.getNextRetryAt())
                .isCloseTo(expectedNextRetry, within(1, ChronoUnit.SECONDS));
    }

    private OutboxEvent persistProcessableOutbox(String payload) {
        OutboxEvent event = OutboxEvent.init("ORDER", 99_999L, payload);
        OutboxEvent saved = outboxEventRepository.save(event);
        outboxEventRepository.flush();
        saved.assignEventIdFromPrimaryKey();
        saved = outboxEventRepository.save(saved);
        createdOutboxIds.add(saved.getId());
        return saved;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
