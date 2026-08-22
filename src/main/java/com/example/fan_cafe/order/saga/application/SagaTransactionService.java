package com.example.fan_cafe.order.saga.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
import com.example.fan_cafe.order.saga.domain.PaymentSagaStateMachine;
import com.example.fan_cafe.order.saga.domain.SagaInstance;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import com.example.fan_cafe.order.saga.exception.SagaErrorCode;
import com.example.fan_cafe.order.saga.infrastructure.SagaInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SagaTransactionService {
    private final OrderRepository orderRepository;
    private final SagaInstanceRepository sagaRepository;
    private final PaymentSagaStateMachine stateMachine;

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
        return SagaSnapshot.from(saga);
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
