package com.example.fan_cafe.outbox;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.outbox.application.OutboxAdminService;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxAdminServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private OutboxAdminService outboxAdminService;

    @Test
    @DisplayName("MANUAL_REQUIRED 이벤트 수동 재시도 요청 시 상태가 FAILED로 변경된다.")
    void requestManualRetry_shouldChangeStatusToFailed() {
        OutboxEvent event = OutboxEvent.init("ORDER", 1L, "{\"eventType\":\"ORDER_CREATED\"}");
        ReflectionTestUtils.setField(event, "id", 1L);
        ReflectionTestUtils.setField(event, "status", OutboxEventStatus.MANUAL_REQUIRED);

        when(outboxEventRepository.findById(1L)).thenReturn(Optional.of(event));

        outboxAdminService.requestManualRetry(1L);

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
    }

    @Test
    @DisplayName("수동 재시도 요청 시 nextRetryAt이 현재 시각으로 갱신된다.")
    void requestManualRetry_shouldUpdateNextRetryAtToNow() {
        OutboxEvent event = OutboxEvent.init("ORDER", 1L, "{\"eventType\":\"ORDER_CREATED\"}");
        ReflectionTestUtils.setField(event, "id", 1L);
        ReflectionTestUtils.setField(event, "status", OutboxEventStatus.MANUAL_REQUIRED);
        ReflectionTestUtils.setField(event, "nextRetryAt", null);

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        when(outboxEventRepository.findById(1L)).thenReturn(Optional.of(event));

        outboxAdminService.requestManualRetry(1L);

        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        assertThat(event.getNextRetryAt()).isAfterOrEqualTo(before);
        assertThat(event.getNextRetryAt()).isBeforeOrEqualTo(after);
    }

    @Test
    @DisplayName("수동 재시도 요청 시 payload, lastError, retryCount는 보존된다.")
    void requestManualRetry_shouldPreservePayloadLastErrorRetryCount() {
        String payload = "{\"eventType\":\"ORDER_CREATED\",\"orderId\":10}";
        String lastError = "[MQ_TIMEOUT] timeout";
        Integer retryCount = 6;

        OutboxEvent event = OutboxEvent.init("ORDER", 10L, payload);
        ReflectionTestUtils.setField(event, "id", 1L);
        ReflectionTestUtils.setField(event, "status", OutboxEventStatus.MANUAL_REQUIRED);
        ReflectionTestUtils.setField(event, "lastError", lastError);
        ReflectionTestUtils.setField(event, "retryCount", retryCount);

        when(outboxEventRepository.findById(1L)).thenReturn(Optional.of(event));

        outboxAdminService.requestManualRetry(1L);

        assertThat(event.getPayload()).isEqualTo(payload);
        assertThat(event.getLastError()).isEqualTo(lastError);
        assertThat(event.getRetryCount()).isEqualTo(retryCount);
    }

    @Test
    @DisplayName("MANUAL_REQUIRED가 아닌 이벤트 수동 재시도 요청 시 예외가 발생한다.")
    void requestManualRetry_shouldThrowExceptionWhenStatusIsNotManualRequired() {
        OutboxEvent event = OutboxEvent.init("ORDER", 1L, "{\"eventType\":\"ORDER_CREATED\"}");
        ReflectionTestUtils.setField(event, "id", 1L);
        ReflectionTestUtils.setField(event, "status", OutboxEventStatus.FAILED);

        when(outboxEventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> outboxAdminService.requestManualRetry(1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("MANUAL_REQUIRED");
    }
}
