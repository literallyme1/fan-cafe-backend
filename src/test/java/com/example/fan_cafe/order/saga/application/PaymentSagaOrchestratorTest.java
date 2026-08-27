package com.example.fan_cafe.order.saga.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import com.example.fan_cafe.order.interfaces.dto.OrderQueryResponse;
import com.example.fan_cafe.order.payment.client.PaymentClient;
import com.example.fan_cafe.order.payment.client.PaymentOutcomeUnknownException;
import com.example.fan_cafe.order.payment.client.PaymentResultResponse;
import com.example.fan_cafe.order.payment.client.PaymentResultStatus;
import com.example.fan_cafe.order.payment.client.PaymentStatusResponse;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import com.example.fan_cafe.order.saga.domain.SagaStep;
import com.example.fan_cafe.order.saga.domain.SagaCompensationReason;
import com.example.fan_cafe.order.saga.exception.OrderCompletionFailedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentSagaOrchestratorTest {
    private static final LocalDateTime APPROVED_AT = LocalDateTime.of(2026, 8, 27, 1, 2, 3);
    @Mock private SagaTransactionService sagaTransactionService;
    @Mock private SagaOrderCompletionService completionService;
    @Mock private SagaCompensationService compensationService;
    @Mock private SagaPaymentFailureService paymentFailureService;
    @Mock private PaymentClient paymentClient;
    @InjectMocks private PaymentSagaOrchestrator orchestrator;

    @Test
    void happyPath_invokesPersistedMilestonesInOrder() {
        UUID sagaId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        SagaSnapshot started = snapshot(sagaId, SagaStatus.STARTED, SagaStep.PAYMENT_APPROVAL);
        SagaSnapshot pending = snapshot(sagaId, SagaStatus.PAYMENT_PENDING, SagaStep.PAYMENT_APPROVAL);
        SagaSnapshot paymentCompleted = snapshot(sagaId, SagaStatus.PAYMENT_COMPLETED, SagaStep.ORDER_COMPLETION);
        PaymentResultResponse approved = new PaymentResultResponse(
                10L, PaymentResultStatus.APPROVED, "pay-1", null, null, APPROVED_AT);
        OrderQueryResponse completedOrder = mock(OrderQueryResponse.class);

        when(sagaTransactionService.start(10L)).thenReturn(started);
        when(sagaTransactionService.transition(sagaId, SagaStatus.PAYMENT_PENDING)).thenReturn(pending);
        when(paymentClient.approve(10L, BigDecimal.TEN, BigDecimal.TEN, "pay-1")).thenReturn(approved);
        when(sagaTransactionService.markPaymentCompleted(sagaId, APPROVED_AT))
                .thenReturn(paymentCompleted);
        when(completionService.complete(sagaId, 10L, "mock payment approved", APPROVED_AT))
                .thenReturn(completedOrder);

        OrderQueryResponse result = orchestrator.approve(
                10L, BigDecimal.TEN, BigDecimal.TEN, "pay-1");

        assertThat(result).isSameAs(completedOrder);
        InOrder order = inOrder(sagaTransactionService, paymentClient, completionService);
        order.verify(sagaTransactionService).start(10L);
        order.verify(sagaTransactionService).transition(sagaId, SagaStatus.PAYMENT_PENDING);
        order.verify(paymentClient).approve(10L, BigDecimal.TEN, BigDecimal.TEN, "pay-1");
        order.verify(sagaTransactionService).markPaymentCompleted(sagaId, APPROVED_AT);
        order.verify(completionService).complete(sagaId, 10L, "mock payment approved", APPROVED_AT);
    }

    @Test
    void lateConcurrentRequestTreatsAdvancedMilestoneAsIdempotentSuccess() {
        UUID sagaId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        SagaSnapshot started = snapshot(sagaId, SagaStatus.STARTED, SagaStep.PAYMENT_APPROVAL);
        SagaSnapshot paymentCompleted = snapshot(
                sagaId, SagaStatus.PAYMENT_COMPLETED, SagaStep.ORDER_COMPLETION);
        OrderQueryResponse completedOrder = mock(OrderQueryResponse.class);

        when(sagaTransactionService.start(10L)).thenReturn(paymentCompleted);
        when(completionService.complete(sagaId, 10L, "mock payment approved"))
                .thenReturn(completedOrder);

        OrderQueryResponse result = orchestrator.approve(
                10L, BigDecimal.TEN, BigDecimal.TEN, "pay-1");

        assertThat(result).isSameAs(completedOrder);
        verifyNoInteractions(paymentClient);
        verify(sagaTransactionService, never()).transition(any(), any());
        verify(sagaTransactionService, never()).advanceToMilestone(any(), any());
    }

    @Test
    void orchestratorDoesNotDeclareTransactionBoundary() {
        assertThat(PaymentSagaOrchestrator.class.isAnnotationPresent(Transactional.class)).isFalse();
    }

    @Test
    void orderCompletionFailureStartsCompensationAndRethrows() {
        UUID sagaId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        SagaSnapshot paymentCompleted = snapshot(
                sagaId, SagaStatus.PAYMENT_COMPLETED, SagaStep.ORDER_COMPLETION);
        OrderCompletionFailedException failure = new OrderCompletionFailedException(
                new IllegalStateException("order completion failed"));

        when(sagaTransactionService.start(10L)).thenReturn(paymentCompleted);
        when(completionService.complete(sagaId, 10L, "mock payment approved"))
                .thenThrow(failure);

        assertThatThrownBy(() -> orchestrator.approve(
                10L, BigDecimal.TEN, BigDecimal.TEN, "pay-1"))
                .isSameAs(failure);

        verify(compensationService).startAfterOrderCompletionFailure(
                sagaId, 10L, SagaCompensationReason.ORDER_FAILURE.name(), null);
        verifyNoInteractions(paymentClient);
    }

    @Test
    void paymentApprovalFailureDoesNotStartCompensation() {
        UUID sagaId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        SagaSnapshot started = snapshot(sagaId, SagaStatus.STARTED, SagaStep.PAYMENT_APPROVAL);
        SagaSnapshot pending = snapshot(sagaId, SagaStatus.PAYMENT_PENDING, SagaStep.PAYMENT_APPROVAL);
        IllegalStateException paymentFailure = new IllegalStateException("payment boundary failed");

        when(sagaTransactionService.start(10L)).thenReturn(started);
        when(sagaTransactionService.transition(sagaId, SagaStatus.PAYMENT_PENDING))
                .thenReturn(pending);
        when(paymentClient.approve(10L, BigDecimal.TEN, BigDecimal.TEN, "pay-1"))
                .thenThrow(paymentFailure);

        assertThatThrownBy(() -> orchestrator.approve(
                10L, BigDecimal.TEN, BigDecimal.TEN, "pay-1"))
                .isSameAs(paymentFailure);

        verifyNoInteractions(compensationService, completionService);
    }

    @Test
    void approvalUnknown_isPersistedBeforeStatusLookup_andNotCompensated() {
        UUID sagaId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        SagaSnapshot started = snapshot(sagaId, SagaStatus.STARTED, SagaStep.PAYMENT_APPROVAL);
        SagaSnapshot pending = snapshot(sagaId, SagaStatus.PAYMENT_PENDING, SagaStep.PAYMENT_APPROVAL);
        SagaSnapshot unknown = snapshot(sagaId, SagaStatus.PAYMENT_UNKNOWN, SagaStep.PAYMENT_STATUS_CHECK);
        PaymentOutcomeUnknownException timeout = new PaymentOutcomeUnknownException(
                OrderErrorCode.PAYMENT_SERVICE_UNAVAILABLE);
        CustomException notFound = new CustomException(OrderErrorCode.PAYMENT_NOT_FOUND);

        when(sagaTransactionService.start(10L)).thenReturn(started);
        when(sagaTransactionService.transition(sagaId, SagaStatus.PAYMENT_PENDING)).thenReturn(pending);
        when(paymentClient.approve(10L, BigDecimal.TEN, BigDecimal.TEN, "pay-1"))
                .thenThrow(timeout);
        when(sagaTransactionService.markPaymentUnknown(eq(sagaId), anyString())).thenReturn(unknown);
        when(paymentClient.getStatus(10L)).thenThrow(notFound);

        assertThatThrownBy(() -> orchestrator.approve(
                10L, BigDecimal.TEN, BigDecimal.TEN, "pay-1"))
                .isSameAs(notFound);

        InOrder order = inOrder(sagaTransactionService, paymentClient);
        order.verify(paymentClient).approve(10L, BigDecimal.TEN, BigDecimal.TEN, "pay-1");
        order.verify(sagaTransactionService).markPaymentUnknown(eq(sagaId), anyString());
        order.verify(paymentClient).getStatus(10L);
        verifyNoInteractions(compensationService, paymentFailureService, completionService);
    }

    @Test
    void existingUnknownSaga_approvedLookupReusesNormalCompletion() {
        UUID sagaId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        SagaSnapshot unknown = snapshot(sagaId, SagaStatus.PAYMENT_UNKNOWN, SagaStep.PAYMENT_STATUS_CHECK);
        SagaSnapshot paymentCompleted = snapshot(
                sagaId, SagaStatus.PAYMENT_COMPLETED, SagaStep.ORDER_COMPLETION);
        PaymentStatusResponse approved = paymentStatus(PaymentResultStatus.APPROVED);
        OrderQueryResponse completed = mock(OrderQueryResponse.class);

        when(sagaTransactionService.start(10L)).thenReturn(unknown);
        when(paymentClient.getStatus(10L)).thenReturn(approved);
        when(compensationService.startLateSuccessIfOrderCannotComplete(sagaId, 10L))
                .thenReturn(Optional.empty());
        when(sagaTransactionService.markPaymentCompleted(sagaId, APPROVED_AT))
                .thenReturn(paymentCompleted);
        when(completionService.complete(sagaId, 10L, "mock payment approved", APPROVED_AT))
                .thenReturn(completed);

        assertThat(orchestrator.approve(10L, BigDecimal.TEN, BigDecimal.TEN, "pay-1"))
                .isSameAs(completed);

        verify(paymentClient, never()).approve(anyLong(), any(), any(), anyString());
        InOrder order = inOrder(
                paymentClient, compensationService, sagaTransactionService, completionService);
        order.verify(paymentClient).getStatus(10L);
        order.verify(compensationService).startLateSuccessIfOrderCannotComplete(sagaId, 10L);
        order.verify(sagaTransactionService).markPaymentCompleted(sagaId, APPROVED_AT);
        order.verify(completionService)
                .complete(sagaId, 10L, "mock payment approved", APPROVED_AT);
    }

    @Test
    void lateApprovedUnknownSagaWithCancelledOrderStartsCompensationWithoutCompletion() {
        UUID sagaId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        SagaSnapshot unknown = snapshot(sagaId, SagaStatus.PAYMENT_UNKNOWN, SagaStep.PAYMENT_STATUS_CHECK);
        OrderQueryResponse cancelled = mock(OrderQueryResponse.class);
        when(sagaTransactionService.start(10L)).thenReturn(unknown);
        when(paymentClient.getStatus(10L)).thenReturn(paymentStatus(PaymentResultStatus.APPROVED));
        when(compensationService.startLateSuccessIfOrderCannotComplete(sagaId, 10L))
                .thenReturn(Optional.of(cancelled));

        assertThat(orchestrator.approve(10L, BigDecimal.TEN, BigDecimal.TEN, "pay-1"))
                .isSameAs(cancelled);

        verify(sagaTransactionService, never()).markPaymentCompleted(sagaId, APPROVED_AT);
        verifyNoInteractions(completionService);
    }

    @Test
    void existingUnknownSaga_failedLookupCancelsThroughFailureService() {
        UUID sagaId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        SagaSnapshot unknown = snapshot(sagaId, SagaStatus.PAYMENT_UNKNOWN, SagaStep.PAYMENT_STATUS_CHECK);
        PaymentStatusResponse failed = paymentStatus(PaymentResultStatus.FAILED);
        OrderQueryResponse cancelled = mock(OrderQueryResponse.class);

        when(sagaTransactionService.start(10L)).thenReturn(unknown);
        when(paymentClient.getStatus(10L)).thenReturn(failed);
        when(paymentFailureService.fail(sagaId, 10L, "declined")).thenReturn(cancelled);

        assertThat(orchestrator.approve(10L, BigDecimal.TEN, BigDecimal.TEN, "pay-1"))
                .isSameAs(cancelled);
        verify(paymentFailureService).fail(sagaId, 10L, "declined");
        verifyNoInteractions(completionService, compensationService);
    }

    @Test
    void unresolvedStatusKeepsUnknownWithoutCancellationOrCompensation() {
        UUID sagaId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        SagaSnapshot unknown = snapshot(sagaId, SagaStatus.PAYMENT_UNKNOWN, SagaStep.PAYMENT_STATUS_CHECK);
        when(sagaTransactionService.start(10L)).thenReturn(unknown);
        when(paymentClient.getStatus(10L)).thenReturn(paymentStatus(PaymentResultStatus.PENDING));

        assertThatThrownBy(() -> orchestrator.approve(
                10L, BigDecimal.TEN, BigDecimal.TEN, "pay-1"))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(OrderErrorCode.PAYMENT_SERVICE_ERROR);

        verifyNoInteractions(paymentFailureService, compensationService, completionService);
        verify(sagaTransactionService, never()).transition(sagaId, SagaStatus.CANCELLED);
    }

    @Test
    void explicitApprovalFailureUsesDefinitiveCancellationPath() {
        UUID sagaId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        SagaSnapshot pending = snapshot(sagaId, SagaStatus.PAYMENT_PENDING, SagaStep.PAYMENT_APPROVAL);
        PaymentResultResponse failed = new PaymentResultResponse(
                10L, PaymentResultStatus.FAILED, null, "declined", null);
        OrderQueryResponse cancelled = mock(OrderQueryResponse.class);
        when(sagaTransactionService.start(10L)).thenReturn(pending);
        when(paymentClient.approve(10L, BigDecimal.TEN, BigDecimal.TEN, "pay-1"))
                .thenReturn(failed);
        when(paymentFailureService.fail(sagaId, 10L, "declined")).thenReturn(cancelled);

        assertThat(orchestrator.approve(10L, BigDecimal.TEN, BigDecimal.TEN, "pay-1"))
                .isSameAs(cancelled);
        verify(paymentFailureService).fail(sagaId, 10L, "declined");
        verifyNoInteractions(completionService, compensationService);
    }

    private PaymentStatusResponse paymentStatus(PaymentResultStatus status) {
        return new PaymentStatusResponse(
                10L, status, BigDecimal.TEN, BigDecimal.TEN,
                status == PaymentResultStatus.APPROVED ? APPROVED_AT : null, "pay-1",
                status == PaymentResultStatus.FAILED ? "declined" : null,
                null, null, null);
    }

    private SagaSnapshot snapshot(UUID sagaId, SagaStatus status, SagaStep step) {
        return new SagaSnapshot(sagaId, 10L, status, step);
    }
}
