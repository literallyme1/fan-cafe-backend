package com.example.fan_cafe.order.saga.application;

import com.example.fan_cafe.order.application.OrderPaymentResultService;
import com.example.fan_cafe.order.interfaces.dto.OrderQueryResponse;
import com.example.fan_cafe.order.payment.client.PaymentClient;
import com.example.fan_cafe.order.payment.client.PaymentResultResponse;
import com.example.fan_cafe.order.payment.client.PaymentResultStatus;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import com.example.fan_cafe.order.saga.domain.SagaStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentSagaOrchestratorTest {
    @Mock private SagaTransactionService sagaTransactionService;
    @Mock private SagaOrderCompletionService completionService;
    @Mock private PaymentClient paymentClient;
    @Mock private OrderPaymentResultService orderPaymentResultService;
    @InjectMocks private PaymentSagaOrchestrator orchestrator;

    @Test
    void happyPath_invokesPersistedMilestonesInOrder() {
        UUID sagaId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        SagaSnapshot started = snapshot(sagaId, SagaStatus.STARTED, SagaStep.PAYMENT_APPROVAL);
        SagaSnapshot pending = snapshot(sagaId, SagaStatus.PAYMENT_PENDING, SagaStep.PAYMENT_APPROVAL);
        SagaSnapshot paymentCompleted = snapshot(sagaId, SagaStatus.PAYMENT_COMPLETED, SagaStep.ORDER_COMPLETION);
        PaymentResultResponse approved = new PaymentResultResponse(
                10L, PaymentResultStatus.APPROVED, "pay-1", null, null);
        OrderQueryResponse completedOrder = mock(OrderQueryResponse.class);

        when(sagaTransactionService.start(10L)).thenReturn(started);
        when(sagaTransactionService.advanceToMilestone(sagaId, SagaStatus.PAYMENT_PENDING)).thenReturn(pending);
        when(paymentClient.approve(10L, BigDecimal.TEN, BigDecimal.TEN, "pay-1")).thenReturn(approved);
        when(sagaTransactionService.advanceToMilestone(sagaId, SagaStatus.PAYMENT_COMPLETED))
                .thenReturn(paymentCompleted);
        when(completionService.complete(sagaId, 10L, "mock payment approved")).thenReturn(completedOrder);

        OrderQueryResponse result = orchestrator.approve(
                10L, BigDecimal.TEN, BigDecimal.TEN, "pay-1");

        assertThat(result).isSameAs(completedOrder);
        InOrder order = inOrder(sagaTransactionService, paymentClient, completionService);
        order.verify(sagaTransactionService).start(10L);
        order.verify(sagaTransactionService).advanceToMilestone(sagaId, SagaStatus.PAYMENT_PENDING);
        order.verify(paymentClient).approve(10L, BigDecimal.TEN, BigDecimal.TEN, "pay-1");
        order.verify(sagaTransactionService).advanceToMilestone(sagaId, SagaStatus.PAYMENT_COMPLETED);
        order.verify(completionService).complete(sagaId, 10L, "mock payment approved");
    }

    @Test
    void lateConcurrentRequestTreatsAdvancedMilestoneAsIdempotentSuccess() {
        UUID sagaId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        SagaSnapshot started = snapshot(sagaId, SagaStatus.STARTED, SagaStep.PAYMENT_APPROVAL);
        SagaSnapshot paymentCompleted = snapshot(
                sagaId, SagaStatus.PAYMENT_COMPLETED, SagaStep.ORDER_COMPLETION);
        OrderQueryResponse completedOrder = mock(OrderQueryResponse.class);

        when(sagaTransactionService.start(10L)).thenReturn(started);
        when(sagaTransactionService.advanceToMilestone(sagaId, SagaStatus.PAYMENT_PENDING))
                .thenReturn(paymentCompleted);
        when(completionService.complete(sagaId, 10L, "mock payment approved"))
                .thenReturn(completedOrder);

        OrderQueryResponse result = orchestrator.approve(
                10L, BigDecimal.TEN, BigDecimal.TEN, "pay-1");

        assertThat(result).isSameAs(completedOrder);
        verifyNoInteractions(paymentClient);
        verify(sagaTransactionService, never())
                .advanceToMilestone(sagaId, SagaStatus.PAYMENT_COMPLETED);
    }

    @Test
    void orchestratorDoesNotDeclareTransactionBoundary() {
        assertThat(PaymentSagaOrchestrator.class.isAnnotationPresent(Transactional.class)).isFalse();
    }

    private SagaSnapshot snapshot(UUID sagaId, SagaStatus status, SagaStep step) {
        return new SagaSnapshot(sagaId, 10L, status, step);
    }
}
