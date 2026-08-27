package com.example.fan_cafe.order.saga.domain;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.saga.exception.SagaErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentSagaStateMachineTest {
    private static final Instant TRANSITION_TIME = Instant.parse("2026-08-23T03:00:00.123456789Z");
    private static final LocalDateTime EXPECTED_TRANSITION_TIME =
            LocalDateTime.ofInstant(TRANSITION_TIME, ZoneOffset.UTC).withNano(123_456_000);
    private final PaymentSagaStateMachine stateMachine = new PaymentSagaStateMachine(
            Clock.fixed(TRANSITION_TIME, ZoneOffset.UTC));

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
        assertThat(saga.getResolvedAt()).isNull();

        stateMachine.transition(saga, SagaStatus.COMPLETED);
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPLETED);
        assertThat(saga.getCurrentStep()).isEqualTo(SagaStep.DONE);
        assertThat(saga.getResolvedAt()).isEqualTo(EXPECTED_TRANSITION_TIME);
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

    @Test
    void compensationPath_transitionsInAllowedOrder() {
        SagaInstance saga = SagaInstance.started(10L);
        stateMachine.transition(saga, SagaStatus.PAYMENT_PENDING);
        stateMachine.transition(saga, SagaStatus.PAYMENT_COMPLETED);

        stateMachine.transition(saga, SagaStatus.COMPENSATING);
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATING);
        assertThat(saga.getCurrentStep()).isEqualTo(SagaStep.PAYMENT_REFUND);

        stateMachine.transition(saga, SagaStatus.COMPENSATED);
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATED);
        assertThat(saga.getCurrentStep()).isEqualTo(SagaStep.DONE);
    }

    @Test
    void compensationReverseTransition_isRejected() {
        SagaInstance saga = SagaInstance.started(10L);
        stateMachine.transition(saga, SagaStatus.PAYMENT_PENDING);
        stateMachine.transition(saga, SagaStatus.PAYMENT_COMPLETED);
        stateMachine.transition(saga, SagaStatus.COMPENSATING);

        assertThatThrownBy(() -> stateMachine.transition(saga, SagaStatus.PAYMENT_COMPLETED))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(SagaErrorCode.INVALID_SAGA_TRANSITION);
    }

    @Test
    void unknownPaymentPath_transitionsThroughExplicitStates() {
        SagaInstance saga = SagaInstance.started(10L);
        stateMachine.transition(saga, SagaStatus.PAYMENT_PENDING);

        stateMachine.transition(saga, SagaStatus.PAYMENT_UNKNOWN);
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.PAYMENT_UNKNOWN);
        assertThat(saga.getCurrentStep()).isEqualTo(SagaStep.PAYMENT_STATUS_CHECK);

        stateMachine.transition(saga, SagaStatus.PAYMENT_COMPLETED);
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.PAYMENT_COMPLETED);
        assertThat(saga.getCurrentStep()).isEqualTo(SagaStep.ORDER_COMPLETION);
    }

    @Test
    void lateApprovedPaymentCanCompensateUnknownSagaWithoutReverseTransition() {
        SagaInstance saga = SagaInstance.started(10L);
        stateMachine.transition(saga, SagaStatus.PAYMENT_PENDING);
        stateMachine.transition(saga, SagaStatus.PAYMENT_UNKNOWN);
        assertThat(saga.getPaymentUnknownAt()).isEqualTo(EXPECTED_TRANSITION_TIME);

        stateMachine.transition(saga, SagaStatus.COMPENSATING);

        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATING);
        assertThat(saga.getCurrentStep()).isEqualTo(SagaStep.PAYMENT_REFUND);
        assertThat(saga.getPaymentUnknownAt()).isEqualTo(EXPECTED_TRANSITION_TIME);
        assertThat(saga.getResolvedAt()).isNull();
    }

    @Test
    void definitivePaymentFailure_canCancelPendingOrUnknownSaga() {
        SagaInstance pendingSaga = SagaInstance.started(10L);
        stateMachine.transition(pendingSaga, SagaStatus.PAYMENT_PENDING);
        stateMachine.transition(pendingSaga, SagaStatus.CANCELLED);
        assertThat(pendingSaga.getStatus()).isEqualTo(SagaStatus.CANCELLED);
        assertThat(pendingSaga.getCurrentStep()).isEqualTo(SagaStep.DONE);

        SagaInstance unknownSaga = SagaInstance.started(11L);
        stateMachine.transition(unknownSaga, SagaStatus.PAYMENT_PENDING);
        stateMachine.transition(unknownSaga, SagaStatus.PAYMENT_UNKNOWN);
        stateMachine.transition(unknownSaga, SagaStatus.CANCELLED);
        assertThat(unknownSaga.getStatus()).isEqualTo(SagaStatus.CANCELLED);
        assertThat(unknownSaga.getCurrentStep()).isEqualTo(SagaStep.DONE);
    }

    @Test
    void branchStates_areNotTreatedAsLinearPaymentPendingMilestones() {
        assertThat(SagaStatus.PAYMENT_UNKNOWN.isAtOrAfter(SagaStatus.PAYMENT_PENDING)).isFalse();
        assertThat(SagaStatus.CANCELLED.isAtOrAfter(SagaStatus.PAYMENT_PENDING)).isFalse();
        assertThat(SagaStatus.PAYMENT_UNKNOWN.isAtOrAfter(SagaStatus.PAYMENT_UNKNOWN)).isTrue();
        assertThat(SagaStatus.CANCELLED.isAtOrAfter(SagaStatus.CANCELLED)).isTrue();
    }

    @Test
    void unknownAndCancelledDoNotAllowUnspecifiedTransitions() {
        SagaInstance unknownSaga = SagaInstance.started(10L);
        stateMachine.transition(unknownSaga, SagaStatus.PAYMENT_PENDING);
        stateMachine.transition(unknownSaga, SagaStatus.PAYMENT_UNKNOWN);

        assertThatThrownBy(() -> stateMachine.transition(unknownSaga, SagaStatus.PAYMENT_PENDING))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(SagaErrorCode.INVALID_SAGA_TRANSITION);

        SagaInstance cancelledSaga = SagaInstance.started(11L);
        stateMachine.transition(cancelledSaga, SagaStatus.PAYMENT_PENDING);
        stateMachine.transition(cancelledSaga, SagaStatus.CANCELLED);

        assertThatThrownBy(() -> stateMachine.transition(cancelledSaga, SagaStatus.PAYMENT_COMPLETED))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(SagaErrorCode.INVALID_SAGA_TRANSITION);
    }

    @Test
    void recoveryTargets_canTransitionToManualReconciliation() {
        SagaInstance unknownSaga = SagaInstance.started(10L);
        stateMachine.transition(unknownSaga, SagaStatus.PAYMENT_PENDING);
        stateMachine.transition(unknownSaga, SagaStatus.PAYMENT_UNKNOWN);
        stateMachine.transition(unknownSaga, SagaStatus.RECONCILIATION_REQUIRED);
        assertThat(unknownSaga.getStatus()).isEqualTo(SagaStatus.RECONCILIATION_REQUIRED);
        assertThat(unknownSaga.getCurrentStep()).isEqualTo(SagaStep.MANUAL_RECONCILIATION);

        SagaInstance compensatingSaga = SagaInstance.started(11L);
        stateMachine.transition(compensatingSaga, SagaStatus.PAYMENT_PENDING);
        stateMachine.transition(compensatingSaga, SagaStatus.PAYMENT_COMPLETED);
        stateMachine.transition(compensatingSaga, SagaStatus.COMPENSATING);
        stateMachine.transition(compensatingSaga, SagaStatus.RECONCILIATION_REQUIRED);
        assertThat(compensatingSaga.getStatus()).isEqualTo(SagaStatus.RECONCILIATION_REQUIRED);
        assertThat(compensatingSaga.getCurrentStep()).isEqualTo(SagaStep.MANUAL_RECONCILIATION);

        SagaInstance completionSaga = SagaInstance.started(12L);
        stateMachine.transition(completionSaga, SagaStatus.PAYMENT_PENDING);
        stateMachine.transition(completionSaga, SagaStatus.PAYMENT_COMPLETED);
        stateMachine.transition(completionSaga, SagaStatus.RECONCILIATION_REQUIRED);
        assertThat(completionSaga.getCurrentStep()).isEqualTo(SagaStep.MANUAL_RECONCILIATION);
    }

    @Test
    void reconciliationRequired_allowsOnlyExplicitManualRecoveryRoutes() {
        SagaInstance forwardSaga = SagaInstance.started(10L);
        stateMachine.transition(forwardSaga, SagaStatus.PAYMENT_PENDING);
        stateMachine.transition(forwardSaga, SagaStatus.PAYMENT_UNKNOWN);
        stateMachine.transition(forwardSaga, SagaStatus.RECONCILIATION_REQUIRED);
        stateMachine.transition(forwardSaga, SagaStatus.PAYMENT_COMPLETED);
        assertThat(forwardSaga.getCurrentStep()).isEqualTo(SagaStep.ORDER_COMPLETION);

        SagaInstance compensationSaga = SagaInstance.started(11L);
        stateMachine.transition(compensationSaga, SagaStatus.PAYMENT_PENDING);
        stateMachine.transition(compensationSaga, SagaStatus.PAYMENT_UNKNOWN);
        stateMachine.transition(compensationSaga, SagaStatus.RECONCILIATION_REQUIRED);
        stateMachine.transition(compensationSaga, SagaStatus.COMPENSATING);
        assertThat(compensationSaga.getCurrentStep()).isEqualTo(SagaStep.PAYMENT_REFUND);

        assertThatThrownBy(() -> stateMachine.transition(compensationSaga, SagaStatus.COMPLETED))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(SagaErrorCode.INVALID_SAGA_TRANSITION);
        assertThat(SagaStatus.RECONCILIATION_REQUIRED.isAtOrAfter(SagaStatus.PAYMENT_PENDING))
                .isFalse();
    }

    @Test
    void initialRecoveryScheduleDoesNotIncreaseRetryCount() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 23, 12, 0);

        SagaInstance unknownSaga = SagaInstance.started(10L);
        stateMachine.transition(unknownSaga, SagaStatus.PAYMENT_PENDING);
        stateMachine.transition(unknownSaga, SagaStatus.PAYMENT_UNKNOWN);
        unknownSaga.schedulePaymentUnknownRecovery(now.plusSeconds(10), "approval outcome unknown");
        assertThat(unknownSaga.getNextRetryAt()).isEqualTo(now.plusSeconds(10));
        assertThat(unknownSaga.getRetryCount()).isZero();
        assertThat(unknownSaga.getLastError()).isEqualTo("approval outcome unknown");

        SagaInstance compensatingSaga = SagaInstance.started(11L);
        stateMachine.transition(compensatingSaga, SagaStatus.PAYMENT_PENDING);
        stateMachine.transition(compensatingSaga, SagaStatus.PAYMENT_COMPLETED);
        stateMachine.transition(compensatingSaga, SagaStatus.COMPENSATING);
        compensatingSaga.scheduleInitialRefundResultDeadline(now.plusMinutes(1));
        assertThat(compensatingSaga.getNextRetryAt()).isEqualTo(now.plusMinutes(1));
        assertThat(compensatingSaga.getRetryCount()).isZero();
        assertThat(compensatingSaga.getLastError()).isNull();
    }
}
