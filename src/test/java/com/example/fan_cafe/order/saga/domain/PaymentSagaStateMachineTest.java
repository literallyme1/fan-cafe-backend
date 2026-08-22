package com.example.fan_cafe.order.saga.domain;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.saga.exception.SagaErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentSagaStateMachineTest {
    private final PaymentSagaStateMachine stateMachine = new PaymentSagaStateMachine();

    @Test
    void happyPath_transitionsInAllowedOrder() {
        SagaInstance saga = SagaInstance.started(10L);

        assertThat(saga.getStatus()).isEqualTo(SagaStatus.STARTED);
        stateMachine.transition(saga, SagaStatus.PAYMENT_PENDING);
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.PAYMENT_PENDING);
        assertThat(saga.getCurrentStep()).isEqualTo(SagaStep.PAYMENT_APPROVAL);

        stateMachine.transition(saga, SagaStatus.PAYMENT_COMPLETED);
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.PAYMENT_COMPLETED);
        assertThat(saga.getCurrentStep()).isEqualTo(SagaStep.ORDER_COMPLETION);

        stateMachine.transition(saga, SagaStatus.COMPLETED);
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPLETED);
        assertThat(saga.getCurrentStep()).isEqualTo(SagaStep.DONE);
    }

    @Test
    void skippedTransition_isRejected() {
        SagaInstance saga = SagaInstance.started(10L);

        assertThatThrownBy(() -> stateMachine.transition(saga, SagaStatus.PAYMENT_COMPLETED))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(SagaErrorCode.INVALID_SAGA_TRANSITION);

        assertThat(saga.getStatus()).isEqualTo(SagaStatus.STARTED);
    }
}
