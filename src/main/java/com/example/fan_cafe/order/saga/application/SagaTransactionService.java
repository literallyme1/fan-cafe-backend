package com.example.fan_cafe.order.saga.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
import com.example.fan_cafe.order.saga.domain.PaymentSagaStateMachine;
import com.example.fan_cafe.order.saga.domain.SagaInstance;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import com.example.fan_cafe.order.saga.exception.SagaErrorCode;
import com.example.fan_cafe.order.saga.infrastructure.SagaInstanceRepository;
import com.example.fan_cafe.order.saga.recovery.SagaRecoveryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SagaTransactionService {
    private final OrderRepository orderRepository;
    private final SagaInstanceRepository sagaRepository;
    private final PaymentSagaStateMachine stateMachine;
    private final SagaRecoveryProperties recoveryProperties;
    private final Clock clock;

    @Transactional
    public SagaSnapshot start(Long orderId) {
        orderRepository.findPaymentOrderWithPessimisticLock(orderId)
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));

        SagaInstance saga = sagaRepository.findByOrderId(orderId)
                .orElseGet(() -> sagaRepository.save(SagaInstance.started(orderId)));
        return SagaSnapshot.from(saga);
    }

    @Transactional
    public SagaSnapshot transition(UUID sagaId, SagaStatus target) {
        SagaInstance saga = sagaRepository.findBySagaIdForUpdate(sagaId)
                .orElseThrow(() -> new CustomException(SagaErrorCode.SAGA_NOT_FOUND));
        if (saga.getStatus() != target) {
            stateMachine.transition(saga, target);
        }
        if (target == SagaStatus.PAYMENT_UNKNOWN && saga.getNextRetryAt() == null) {
            saga.schedulePaymentUnknownRecovery(firstPaymentRecoveryAt(), "payment outcome unknown");
        }
        return SagaSnapshot.from(saga);
    }

    @Transactional
    public SagaSnapshot markPaymentUnknown(UUID sagaId, String errorSummary) {
        SagaInstance saga = sagaRepository.findBySagaIdForUpdate(sagaId)
                .orElseThrow(() -> new CustomException(SagaErrorCode.SAGA_NOT_FOUND));
        switch (saga.getStatus()) {
            case PAYMENT_PENDING -> {
                stateMachine.transition(saga, SagaStatus.PAYMENT_UNKNOWN);
                saga.schedulePaymentUnknownRecovery(firstPaymentRecoveryAt(), errorSummary);
            }
            case PAYMENT_UNKNOWN, PAYMENT_COMPLETED, COMPLETED,
                    COMPENSATING, COMPENSATED, CANCELLED, RECONCILIATION_REQUIRED -> {
                // 동시 요청이 이미 분기 또는 후속 상태를 확정했다. 역전이하지 않는다.
            }
            case STARTED -> throw new CustomException(SagaErrorCode.INVALID_SAGA_TRANSITION);
        }
        return SagaSnapshot.from(saga);
    }

    private LocalDateTime firstPaymentRecoveryAt() {
        return LocalDateTime.now(clock).plus(recoveryProperties.getPaymentUnknownInitialDelay());
    }

    @Transactional
    public SagaSnapshot advanceToMilestone(UUID sagaId, SagaStatus milestone) {
        SagaInstance saga = sagaRepository.findBySagaIdForUpdate(sagaId)
                .orElseThrow(() -> new CustomException(SagaErrorCode.SAGA_NOT_FOUND));
        if (!saga.getStatus().isAtOrAfter(milestone)) {
            stateMachine.transition(saga, milestone);
        }
        return SagaSnapshot.from(saga);
    }
}
