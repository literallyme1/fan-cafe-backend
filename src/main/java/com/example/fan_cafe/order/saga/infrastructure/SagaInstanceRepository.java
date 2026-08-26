package com.example.fan_cafe.order.saga.infrastructure;

import com.example.fan_cafe.order.saga.domain.SagaInstance;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface SagaInstanceRepository extends JpaRepository<SagaInstance, UUID> {

    Optional<SagaInstance> findByOrderId(Long orderId);

    void deleteByOrderId(Long orderId);

    List<SagaInstance> findAllByStatusOrderByUpdatedAtDesc(SagaStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SagaInstance s where s.sagaId = :sagaId")
    Optional<SagaInstance> findBySagaIdForUpdate(@Param("sagaId") UUID sagaId);

    @Query(value = """
            SELECT *
            FROM saga_instance
            WHERE status IN ('PAYMENT_PENDING', 'PAYMENT_UNKNOWN', 'COMPENSATING')
              AND next_retry_at <= :now
            ORDER BY next_retry_at, saga_id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<SagaInstance> findNextDueRecoveryForUpdateSkipLocked(
            @Param("now") LocalDateTime now);
}
