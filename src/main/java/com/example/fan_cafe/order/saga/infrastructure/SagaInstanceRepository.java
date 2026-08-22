package com.example.fan_cafe.order.saga.infrastructure;

import com.example.fan_cafe.order.saga.domain.SagaInstance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SagaInstanceRepository extends JpaRepository<SagaInstance, UUID> {

    Optional<SagaInstance> findByOrderId(Long orderId);

    void deleteByOrderId(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SagaInstance s where s.sagaId = :sagaId")
    Optional<SagaInstance> findBySagaIdForUpdate(@Param("sagaId") UUID sagaId);
}
