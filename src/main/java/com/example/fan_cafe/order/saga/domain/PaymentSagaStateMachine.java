package com.example.fan_cafe.order.saga.domain;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.saga.exception.SagaErrorCode;
import org.springframework.stereotype.Component;

@Component
public class PaymentSagaStateMachine {

    public void transition(SagaInstance saga, SagaStatus target) {
        SagaStep nextStep = resolveNextStep(saga.getStatus(), target);
        saga.changeState(target, nextStep);
    }

    private SagaStep resolveNextStep(SagaStatus current, SagaStatus target) {
        if (current == SagaStatus.STARTED && target == SagaStatus.PAYMENT_PENDING) {
            return SagaStep.PAYMENT_APPROVAL;
        }
        if (current == SagaStatus.PAYMENT_PENDING && target == SagaStatus.PAYMENT_COMPLETED) {
            return SagaStep.ORDER_COMPLETION;
        }
        if (current == SagaStatus.PAYMENT_COMPLETED && target == SagaStatus.COMPLETED) {
            return SagaStep.DONE;
        }
        throw new CustomException(SagaErrorCode.INVALID_SAGA_TRANSITION);
    }
}
