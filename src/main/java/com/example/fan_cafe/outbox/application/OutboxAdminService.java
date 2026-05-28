package com.example.fan_cafe.outbox.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.outbox.domain.OutboxEvent;
import com.example.fan_cafe.outbox.domain.OutboxEventStatus;
import com.example.fan_cafe.outbox.exception.OutboxAdminErrorCode;
import com.example.fan_cafe.outbox.infrastructure.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxAdminService {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional(readOnly = true)
    public List<OutboxEvent> getManualRequiredEvents() {
        return outboxEventRepository.findAllByStatusOrderByCreatedAtDesc(OutboxEventStatus.MANUAL_REQUIRED);
    }

    @Transactional(readOnly = true)
    public OutboxEvent getOutboxEvent(Long id) {
        return outboxEventRepository.findById(id)
                .orElseThrow(() -> new CustomException(OutboxAdminErrorCode.OUTBOX_EVENT_NOT_FOUND));
    }

    @Transactional
    public void requestManualRetry(Long id) {
        OutboxEvent event = outboxEventRepository.findById(id)
                .orElseThrow(() -> new CustomException(OutboxAdminErrorCode.OUTBOX_EVENT_NOT_FOUND));

        if (!event.isManualRequired()) {
            throw new CustomException(OutboxAdminErrorCode.OUTBOX_EVENT_NOT_MANUAL_REQUIRED);
        }

        event.markManualRetryRequested(LocalDateTime.now());
        log.info("[OUTBOX ADMIN] manual retry requested outboxEventId={}", id);
    }
}
