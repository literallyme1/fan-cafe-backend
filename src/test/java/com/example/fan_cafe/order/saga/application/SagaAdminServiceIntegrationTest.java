package com.example.fan_cafe.order.saga.application;

import com.example.fan_cafe.order.domain.Status;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
import com.example.fan_cafe.order.payment.client.PaymentClient;
import com.example.fan_cafe.order.payment.client.PaymentResultStatus;
import com.example.fan_cafe.order.payment.client.PaymentStatusResponse;
import com.example.fan_cafe.order.saga.domain.PaymentSagaStateMachine;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import com.example.fan_cafe.order.saga.infrastructure.SagaInstanceRepository;
import com.example.fan_cafe.order.saga.messaging.PaymentRefundedResult;
import com.example.fan_cafe.order.support.OrderIntegrationTestSupport;
import com.example.fan_cafe.order.support.OrderIntegrationTestSupport.PaymentPendingFixture;
import com.example.fan_cafe.outbox.infrastructure.OutboxEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("integration")
@SpringBootTest(properties = "spring.jpa.open-in-view=false")
@ActiveProfiles("ci")
class SagaAdminServiceIntegrationTest {
    @Autowired private SagaAdminService sagaAdminService;
    @Autowired private SagaTransactionService sagaTransactionService;
    @Autowired private SagaCompensationService compensationService;
    @Autowired private SagaInstanceRepository sagaRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OutboxEventRepository outboxRepository;
    @Autowired private PaymentSagaStateMachine stateMachine;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private OrderIntegrationTestSupport fixtures;
    @MockitoBean private PaymentClient paymentClient;

    private PaymentPendingFixture fixture;

    @AfterEach
    void tearDown() {
        fixtures.cleanup(fixture);
    }

    @Test
    void reconciliationRequiredSagaIsVisibleInAdminListAndDetail() {
        fixture = fixtures.createPaymentPendingOrder();
        SagaSnapshot reconciled = makeUnknownReconciliation();

        assertThat(sagaAdminService.getReconciliationRequiredSagas())
                .anySatisfy(saga -> {
                    assertThat(saga.sagaId()).isEqualTo(reconciled.sagaId());
                    assertThat(saga.status()).isEqualTo(SagaStatus.RECONCILIATION_REQUIRED);
                    assertThat(saga.retryCount()).isEqualTo(3);
                    assertThat(saga.lastError()).isEqualTo("automatic recovery exhausted");
                });
        assertThat(sagaAdminService.getSaga(reconciled.sagaId()).lastError())
                .isEqualTo("automatic recovery exhausted");
    }

    @Test
    void paymentUnknownStatusRecheckUsesLookupOnlyAndReusesNormalCompletion() {
        fixture = fixtures.createPaymentPendingOrder();
        SagaSnapshot unknown = makeUnknown();
        when(paymentClient.getStatus(fixture.order().getId())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return paymentStatus(PaymentResultStatus.APPROVED);
        });

        sagaAdminService.requestAction(unknown.sagaId(), SagaAdminAction.RECHECK_PAYMENT_STATUS);

        assertThat(sagaRepository.findById(unknown.sagaId()).orElseThrow().getStatus())
                .isEqualTo(SagaStatus.COMPLETED);
        assertThat(orderRepository.findById(fixture.order().getId()).orElseThrow().getStatus())
                .isEqualTo(Status.PAID);
        verify(paymentClient).getStatus(fixture.order().getId());
        verify(paymentClient, never()).approve(anyLong(), any(), any(), anyString());
    }

    @Test
    void approvedReconciliationCanResumeForwardThroughExplicitFsmRoute() {
        fixture = fixtures.createPaymentPendingOrder();
        SagaSnapshot reconciled = makeUnknownReconciliation();
        when(paymentClient.getStatus(fixture.order().getId()))
                .thenReturn(paymentStatus(PaymentResultStatus.APPROVED));

        sagaAdminService.requestAction(reconciled.sagaId(), SagaAdminAction.RESUME_FORWARD);

        assertThat(sagaRepository.findById(reconciled.sagaId()).orElseThrow().getStatus())
                .isEqualTo(SagaStatus.COMPLETED);
        assertThat(orderRepository.findById(fixture.order().getId()).orElseThrow().getStatus())
                .isEqualTo(Status.PAID);
        verify(paymentClient, never()).approve(anyLong(), any(), any(), anyString());
    }

    @Test
    void manualCompensationReusesRefundOutboxAndDuplicateRequestDoesNotDuplicateCommand() {
        fixture = fixtures.createPaymentPendingOrder();
        SagaSnapshot reconciled = makeCompensatingReconciliation();
        Long orderId = fixture.order().getId();
        long commandsBefore = refundCommandCount(orderId);
        when(paymentClient.getStatus(orderId))
                .thenReturn(paymentStatus(PaymentResultStatus.APPROVED));

        sagaAdminService.requestAction(reconciled.sagaId(), SagaAdminAction.COMPENSATE);
        sagaAdminService.requestAction(reconciled.sagaId(), SagaAdminAction.COMPENSATE);

        assertThat(sagaRepository.findById(reconciled.sagaId()).orElseThrow().getStatus())
                .isEqualTo(SagaStatus.COMPENSATING);
        assertThat(refundCommandCount(orderId)).isEqualTo(commandsBefore + 1);
        assertThat(outboxRepository.findAll().stream()
                .filter(event -> orderId.equals(event.getAggregateId()))
                .filter(event -> event.getPayload().contains("\"eventType\":\"REFUND_PAYMENT\""))
                .map(event -> event.getPayload().contains("\"idempotencyKey\":\"REFUND:"
                        + reconciled.sagaId() + "\"")))
                .containsOnly(true);

        compensationService.complete(new PaymentRefundedResult(
                PaymentRefundedResult.EVENT_TYPE,
                reconciled.sagaId(),
                orderId,
                PaymentResultStatus.REFUNDED,
                "REFUND:" + reconciled.sagaId(),
                "manual reconciliation refund"));
        compensationService.complete(new PaymentRefundedResult(
                PaymentRefundedResult.EVENT_TYPE,
                reconciled.sagaId(),
                orderId,
                PaymentResultStatus.REFUNDED,
                "REFUND:" + reconciled.sagaId(),
                "manual reconciliation refund"));

        assertThat(sagaRepository.findById(reconciled.sagaId()).orElseThrow().getStatus())
                .isEqualTo(SagaStatus.COMPENSATED);
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
                .isEqualTo(Status.REFUNDED);
        verify(paymentClient, times(1)).getStatus(orderId);
    }

    private SagaSnapshot makeUnknown() {
        SagaSnapshot started = sagaTransactionService.start(fixture.order().getId());
        sagaTransactionService.transition(started.sagaId(), SagaStatus.PAYMENT_PENDING);
        return sagaTransactionService.markPaymentUnknown(started.sagaId(), "payment outcome unknown");
    }

    private SagaSnapshot makeUnknownReconciliation() {
        SagaSnapshot unknown = makeUnknown();
        reconcile(unknown.sagaId());
        return sagaTransactionService.get(unknown.sagaId());
    }

    private SagaSnapshot makeCompensatingReconciliation() {
        SagaSnapshot started = sagaTransactionService.start(fixture.order().getId());
        sagaTransactionService.transition(started.sagaId(), SagaStatus.PAYMENT_PENDING);
        sagaTransactionService.transition(started.sagaId(), SagaStatus.PAYMENT_COMPLETED);
        compensationService.start(started.sagaId(), fixture.order().getId(), "completion failed");
        reconcile(started.sagaId());
        return sagaTransactionService.get(started.sagaId());
    }

    private void reconcile(java.util.UUID sagaId) {
        transactionTemplate.executeWithoutResult(ignored -> {
            var saga = sagaRepository.findBySagaIdForUpdate(sagaId).orElseThrow();
            stateMachine.transition(saga, SagaStatus.RECONCILIATION_REQUIRED);
            saga.recordReconciliationFailure(3, "automatic recovery exhausted");
        });
    }

    private PaymentStatusResponse paymentStatus(PaymentResultStatus status) {
        BigDecimal total = fixture.totalPrice();
        return new PaymentStatusResponse(
                fixture.order().getId(), status, total, total, "admin-payment-key",
                null, status == PaymentResultStatus.REFUNDED ? "REFUND:test" : null,
                null, null);
    }

    private long refundCommandCount(Long orderId) {
        return outboxRepository.findAll().stream()
                .filter(event -> orderId.equals(event.getAggregateId()))
                .filter(event -> event.getPayload().contains("\"eventType\":\"REFUND_PAYMENT\""))
                .count();
    }
}
