package com.example.fan_cafe.outbox.application;

import com.example.fan_cafe.outbox.domain.OutboxEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DummyOutboxPublisher implements OutboxPublisher {

    // 실제 MQ 연동 전까지 로그 기반 더미 발행기로 동작한다.
    @Override
    public void publish(OutboxEvent event) {
        log.info("[OUTBOX DUMMY PUBLISH] id={}, aggregateType={}, aggregateId={}",
                event.getId(), event.getAggregateType(), event.getAggregateId());
    }
}
