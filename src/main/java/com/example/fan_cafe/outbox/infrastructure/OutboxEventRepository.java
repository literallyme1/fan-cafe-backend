package com.example.fan_cafe.outbox.infrastructure;

import com.example.fan_cafe.outbox.domain.OutboxEvent;
import com.example.fan_cafe.outbox.domain.OutboxEventStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    // outbox 이벤트 저장/조회는 기본 JPA 메서드로 처리한다.

    // 처리 가능한 outbox 이벤트를 잠금 기반으로 최대 50건 조회한다.
    @Query(value = """
            SELECT *
            FROM outbox_events
            WHERE status IN ('NEW', 'FAILED')
              AND next_retry_at <= :now
            ORDER BY id
            LIMIT 50
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> findProcessableEventsForUpdate(@Param("now") LocalDateTime now);

    List<OutboxEvent> findAllByStatusOrderByCreatedAtDesc(OutboxEventStatus status);

    long countByAggregateTypeAndAggregateId(String aggregateType, Long aggregateId);

    void deleteByAggregateTypeAndAggregateId(String aggregateType, Long aggregateId);
}

