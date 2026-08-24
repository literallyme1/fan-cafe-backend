package com.example.fan_cafe.order.saga.domain;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.saga.exception.SagaErrorCode;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class PaymentSagaStateMachine {
    private final Clock clock;

    public PaymentSagaStateMachine(Clock clock) {
        this.clock = clock;
    }

    public void transition(SagaInstance saga, SagaStatus target) {
        SagaStep nextStep = resolveNextStep(saga.getStatus(), target);
        saga.changeState(
                target,
                nextStep,
                LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS));
    }

    private SagaStep resolveNextStep(SagaStatus current, SagaStatus target) {
        if (current == SagaStatus.STARTED && target == SagaStatus.PAYMENT_PENDING) {
            return SagaStep.PAYMENT_APPROVAL;
        }
        if (current == SagaStatus.PAYMENT_PENDING && target == SagaStatus.PAYMENT_COMPLETED) {
            return SagaStep.ORDER_COMPLETION;
        }
        if (current == SagaStatus.PAYMENT_PENDING && target == SagaStatus.PAYMENT_UNKNOWN) {
            return SagaStep.PAYMENT_STATUS_CHECK;
        }
        if (current == SagaStatus.PAYMENT_PENDING && target == SagaStatus.CANCELLED) {
            return SagaStep.DONE;
        }
        if (current == SagaStatus.PAYMENT_UNKNOWN && target == SagaStatus.PAYMENT_COMPLETED) {
            return SagaStep.ORDER_COMPLETION;
        }
        if (current == SagaStatus.PAYMENT_UNKNOWN && target == SagaStatus.CANCELLED) {
            return SagaStep.DONE;
        }
        if (current == SagaStatus.PAYMENT_UNKNOWN && target == SagaStatus.COMPENSATING) {
            return SagaStep.PAYMENT_REFUND;
        }
        if (current == SagaStatus.PAYMENT_COMPLETED && target == SagaStatus.COMPLETED) {
            return SagaStep.DONE;
        }
        if (current == SagaStatus.PAYMENT_COMPLETED && target == SagaStatus.COMPENSATING) {
            return SagaStep.PAYMENT_REFUND;
        }
        if (current == SagaStatus.COMPENSATING && target == SagaStatus.COMPENSATED) {
            return SagaStep.DONE;
        }
        if ((current == SagaStatus.PAYMENT_UNKNOWN || current == SagaStatus.COMPENSATING)
                && target == SagaStatus.RECONCILIATION_REQUIRED) {
            return SagaStep.MANUAL_RECONCILIATION;
        }
        throw new CustomException(SagaErrorCode.INVALID_SAGA_TRANSITION);
    }
}
