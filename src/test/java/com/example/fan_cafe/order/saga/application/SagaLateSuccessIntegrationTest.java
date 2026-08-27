package com.example.fan_cafe.order.saga.application;

import com.example.fan_cafe.order.application.OrderService;
import com.example.fan_cafe.order.domain.Status;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
import com.example.fan_cafe.order.infrastructure.OrderStatusHistoryRepository;
import com.example.fan_cafe.order.payment.client.PaymentClient;
import com.example.fan_cafe.order.payment.client.PaymentResultStatus;
import com.example.fan_cafe.order.payment.client.PaymentStatusResponse;
import com.example.fan_cafe.order.saga.domain.SagaInstance;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import com.example.fan_cafe.order.saga.exception.OrderCompletionFailedException;
import com.example.fan_cafe.order.saga.infrastructure.SagaInstanceRepository;
import com.example.fan_cafe.order.saga.messaging.PaymentRefundedResult;
import com.example.fan_cafe.order.support.OrderIntegrationTestSupport;
import com.example.fan_cafe.order.support.OrderIntegrationTestSupport.PaymentPendingFixture;
import com.example.fan_cafe.merchandise.infrastructure.MerchandiseRepository;
import com.example.fan_cafe.outbox.domain.OutboxEvent;
import com.example.fan_cafe.outbox.infrastructure.OutboxEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Tag("integration")
@ActiveProfiles("ci")
@SpringBootTest(properties = {
        "spring.jpa.open-in-view=false",
        "saga.recovery.enabled=false"
})
class SagaLateSuccessIntegrationTest {

    @Autowired private OrderIntegrationTestSupport fixtures;
    @Autowired private OrderService orderService;
    @Autowired private PaymentSagaOrchestrator orchestrator;
    @Autowired private SagaCompensationService compensationService;
    @Autowired private SagaInstanceRepository sagaRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderStatusHistoryRepository historyRepository;
    @Autowired private MerchandiseRepository merchandiseRepository;
    @MockitoSpyBean private OutboxEventRepository outboxRepository;
    @MockitoSpyBean private SagaTransactionService sagaTransactionService;
    @MockitoBean private PaymentClient paymentClient;

    private PaymentPendingFixture fixture;

    @AfterEach
    void tearDown() {
        if (fixture != null) {
            fixtures.cleanup(fixture);
        }
    }

    @Test
    void cancelledOrderLateApprovalStartsExistingCompensationAndCompletesWithoutRestoringStockTwice() {
        SagaSnapshot unknown = cancelledUnknownSaga();
        Long orderId = fixture.order().getId();
        when(paymentClient.getStatus(orderId)).thenReturn(approvedStatus());

        var response = orchestrator.recoverPaymentUnknown(unknown.sagaId(), orderId);

        assertThat(response.getStatus()).isEqualTo(Status.CANCELLED);
        assertCompensatingWithSingleRefundCommand(unknown.sagaId());

        compensationService.complete(refundedResult(unknown.sagaId()));
        compensationService.complete(refundedResult(unknown.sagaId()));

        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(Status.REFUNDED);
        assertThat(sagaRepository.findById(unknown.sagaId()).orElseThrow().getStatus())
                .isEqualTo(SagaStatus.COMPENSATED);
        assertThat(merchandiseRepository.findById(fixture.merchandise().getId()).orElseThrow().getStock())
                .isEqualTo(100);
        assertThat(historyRepository.countByOrder_Id(orderId)).isEqualTo(2);
        assertThat(outboxRepository.countByAggregateTypeAndAggregateId("ORDER", orderId)).isEqualTo(2);
    }

    @Test
    void compensationOutboxFailureRollsBackLateSuccessSagaTransition() {
        SagaSnapshot unknown = cancelledUnknownSaga();
        Long orderId = fixture.order().getId();
        when(paymentClient.getStatus(orderId)).thenReturn(approvedStatus());
        doThrow(new DataIntegrityViolationException("forced late refund outbox failure"))
                .when(outboxRepository).flush();

        assertThatThrownBy(() -> orchestrator.recoverPaymentUnknown(unknown.sagaId(), orderId))
                .isInstanceOf(DataIntegrityViolationException.class);

        SagaInstance rolledBack = sagaRepository.findById(unknown.sagaId()).orElseThrow();
        assertThat(rolledBack.getStatus()).isEqualTo(SagaStatus.PAYMENT_UNKNOWN);
        assertThat(paymentSagaOutbox()).isEmpty();
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(Status.CANCELLED);
    }

    @Test
    void cancellationWhilePaymentLookupIsInFlightConvergesToCompensation() throws Exception {
        fixture = fixtures.createPaymentPendingOrder();
        SagaSnapshot unknown = makeUnknown();
        Long orderId = fixture.order().getId();
        CountDownLatch lookupStarted = new CountDownLatch(1);
        CountDownLatch cancellationCommitted = new CountDownLatch(1);
        when(paymentClient.getStatus(orderId)).thenAnswer(invocation -> {
            lookupStarted.countDown();
            assertThat(cancellationCommitted.await(10, TimeUnit.SECONDS)).isTrue();
            return approvedStatus();
        });

        var executor = Executors.newSingleThreadExecutor();
        try {
            var recovery = executor.submit(() -> orchestrator.recoverPaymentUnknown(unknown.sagaId(), orderId));
            assertThat(lookupStarted.await(10, TimeUnit.SECONDS)).isTrue();
            orderService.cancel(fixture.user(), orderId);
            cancellationCommitted.countDown();
            assertThat(recovery.get(10, TimeUnit.SECONDS).getStatus()).isEqualTo(Status.CANCELLED);
        } finally {
            cancellationCommitted.countDown();
            executor.shutdownNow();
        }

        assertCompensatingWithSingleRefundCommand(unknown.sagaId());
    }

    @Test
    void paymentCompletionHoldingOrderLockMakesCancellationObserveCommittedPaidState() throws Exception {
        fixture = fixtures.createPaymentPendingOrder();
        SagaSnapshot unknown = makeUnknown();
        Long orderId = fixture.order().getId();
        PaymentStatusResponse approved = approvedStatus();
        when(paymentClient.getStatus(orderId)).thenReturn(approved);
        CountDownLatch completionHoldingOrderLock = new CountDownLatch(1);
        CountDownLatch allowCompletionCommit = new CountDownLatch(1);
        CountDownLatch cancellationStarted = new CountDownLatch(1);
        doAnswer(invocation -> {
            completionHoldingOrderLock.countDown();
            assertThat(allowCompletionCommit.await(10, TimeUnit.SECONDS)).isTrue();
            return invocation.callRealMethod();
        }).when(sagaTransactionService)
                .transition(eq(unknown.sagaId()), eq(SagaStatus.COMPLETED));

        var executor = Executors.newFixedThreadPool(2);
        try {
            var completion = executor.submit(
                    () -> orchestrator.recoverPaymentUnknown(unknown.sagaId(), orderId));
            assertThat(completionHoldingOrderLock.await(10, TimeUnit.SECONDS)).isTrue();

            var cancellation = executor.submit(() -> {
                cancellationStarted.countDown();
                return orderService.cancel(fixture.user(), orderId);
            });
            assertThat(cancellationStarted.await(10, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> cancellation.get(500, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);

            allowCompletionCommit.countDown();
            assertThat(completion.get(10, TimeUnit.SECONDS).getStatus()).isEqualTo(Status.PAID);
            assertThatThrownBy(() -> cancellation.get(10, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(CustomException.class);
        } finally {
            allowCompletionCommit.countDown();
            executor.shutdownNow();
        }

        assertThat(approved.status()).isEqualTo(PaymentResultStatus.APPROVED);
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(Status.PAID);
        assertThat(sagaRepository.findById(unknown.sagaId()).orElseThrow().getStatus())
                .isEqualTo(SagaStatus.COMPLETED);
        assertThat(historyRepository.countByOrder_Id(orderId)).isEqualTo(1);
        assertThat(outboxRepository.countByAggregateTypeAndAggregateId("ORDER", orderId)).isEqualTo(1);
        assertThat(merchandiseRepository.findById(fixture.merchandise().getId()).orElseThrow().getStock())
                .isEqualTo(98);
    }

    @Test
    void cancellationAfterEligibilityCheckFallsBackToExistingStep4Compensation() throws Exception {
        fixture = fixtures.createPaymentPendingOrder();
        SagaSnapshot unknown = makeUnknown();
        Long orderId = fixture.order().getId();
        when(paymentClient.getStatus(orderId)).thenReturn(approvedStatus());
        CountDownLatch milestoneReached = new CountDownLatch(1);
        CountDownLatch cancellationCommitted = new CountDownLatch(1);
        doAnswer(invocation -> {
            milestoneReached.countDown();
            assertThat(cancellationCommitted.await(10, TimeUnit.SECONDS)).isTrue();
            return invocation.callRealMethod();
        }).when(sagaTransactionService)
                .markPaymentCompleted(eq(unknown.sagaId()), any(LocalDateTime.class));

        var executor = Executors.newSingleThreadExecutor();
        try {
            var recovery = executor.submit(() -> orchestrator.recoverPaymentUnknown(unknown.sagaId(), orderId));
            assertThat(milestoneReached.await(10, TimeUnit.SECONDS)).isTrue();
            orderService.cancel(fixture.user(), orderId);
            cancellationCommitted.countDown();
            assertThatThrownBy(() -> recovery.get(10, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(OrderCompletionFailedException.class);
        } finally {
            cancellationCommitted.countDown();
            executor.shutdownNow();
        }

        assertCompensatingWithSingleRefundCommand(unknown.sagaId());
    }

    @Test
    void concurrentDuplicateLateApprovalsCreateOneRefundCommandAndOneOrderResult() throws Exception {
        SagaSnapshot unknown = cancelledUnknownSaga();
        Long orderId = fixture.order().getId();
        when(paymentClient.getStatus(orderId)).thenReturn(approvedStatus());
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> {
                start.await();
                return orchestrator.recoverPaymentUnknown(unknown.sagaId(), orderId);
            });
            var second = executor.submit(() -> {
                start.await();
                return orchestrator.recoverPaymentUnknown(unknown.sagaId(), orderId);
            });
            start.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS).getStatus()).isEqualTo(Status.CANCELLED);
            assertThat(second.get(10, TimeUnit.SECONDS).getStatus()).isEqualTo(Status.CANCELLED);
        } finally {
            executor.shutdownNow();
        }

        assertCompensatingWithSingleRefundCommand(unknown.sagaId());
        compensationService.complete(refundedResult(unknown.sagaId()));
        compensationService.complete(refundedResult(unknown.sagaId()));
        assertThat(historyRepository.countByOrder_Id(orderId)).isEqualTo(2);
        assertThat(outboxRepository.countByAggregateTypeAndAggregateId("ORDER", orderId)).isEqualTo(2);
        assertThat(merchandiseRepository.findById(fixture.merchandise().getId()).orElseThrow().getStock())
                .isEqualTo(100);
    }

    private SagaSnapshot cancelledUnknownSaga() {
        fixture = fixtures.createPaymentPendingOrder();
        SagaSnapshot unknown = makeUnknown();
        orderService.cancel(fixture.user(), fixture.order().getId());
        return unknown;
    }

    private SagaSnapshot makeUnknown() {
        SagaSnapshot started = sagaTransactionService.start(fixture.order().getId());
        sagaTransactionService.transition(started.sagaId(), SagaStatus.PAYMENT_PENDING);
        return sagaTransactionService.markPaymentUnknown(started.sagaId(), "late approval pending");
    }

    private PaymentStatusResponse approvedStatus() {
        return new PaymentStatusResponse(
                fixture.order().getId(), PaymentResultStatus.APPROVED,
                fixture.totalPrice(), fixture.totalPrice(), LocalDateTime.now(), "late-payment-key",
                null, null, null, null);
    }

    private PaymentRefundedResult refundedResult(UUID sagaId) {
        return new PaymentRefundedResult(
                PaymentRefundedResult.EVENT_TYPE,
                sagaId,
                fixture.order().getId(),
                PaymentResultStatus.REFUNDED,
                "REFUND:" + sagaId,
                "late payment approval after order cancellation");
    }

    private void assertCompensatingWithSingleRefundCommand(UUID sagaId) {
        SagaInstance saga = sagaRepository.findById(sagaId).orElseThrow();
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATING);
        assertThat(paymentSagaOutbox()).singleElement().satisfies(event -> {
            assertThat(event.getPayload()).contains("\"eventType\":\"REFUND_PAYMENT\"");
            assertThat(event.getPayload()).contains("\"idempotencyKey\":\"REFUND:" + sagaId + "\"");
        });
    }

    private List<OutboxEvent> paymentSagaOutbox() {
        Long orderId = fixture.order().getId();
        return outboxRepository.findAll().stream()
                .filter(event -> "PAYMENT_SAGA".equals(event.getAggregateType()))
                .filter(event -> orderId.equals(event.getAggregateId()))
                .toList();
    }
}
