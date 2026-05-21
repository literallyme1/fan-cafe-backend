package com.example.fan_cafe.outbox.infrastructure;

import com.example.fan_cafe.outbox.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    boolean existsByEventIdAndConsumerType(String eventId, String consumerType);

    long deleteByEventIdAndConsumerType(String eventId, String consumerType);
}
