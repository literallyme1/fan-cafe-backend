package com.example.fan_cafe.order.saga.recovery;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.domain.Status;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
import com.example.fan_cafe.order.payment.client.PaymentClient;
import com.example.fan_cafe.order.payment.client.PaymentResultStatus;
import com.example.fan_cafe.order.payment.client.PaymentStatusResponse;
import com.example.fan_cafe.order.saga.application.PaymentSagaOrchestrator;
import com.example.fan_cafe.order.saga.application.SagaCompensationService;
import com.example.fan_cafe.order.saga.application.SagaSnapshot;
import com.example.fan_cafe.order.saga.application.SagaTransactionService;
import com.example.fan_cafe.order.saga.domain.SagaInstance;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import com.example.fan_cafe.order.saga.infrastructure.SagaInstanceRepository;
import com.example.fan_cafe.order.saga.messaging.RefundPaymentCommand;
import com.example.fan_cafe.order.support.OrderIntegrationTestSupport;
import com.example.fan_cafe.order.support.OrderIntegrationTestSupport.PaymentPendingFixture;
import com.example.fan_cafe.outbox.domain.OutboxEvent;
import com.example.fan_cafe.outbox.infrastructure.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("integration")
@ActiveProfiles("ci")
@SpringBootTest(properties = {
        "spring.jpa.open-in-view=false",
        "saga.recovery.enabled=false",
        "saga.recovery.batch-size=10",
        "saga.recovery.max-retry-count=3",
        "saga.recovery.payment-unknown-initial-delay=10s",
        "saga.recovery.refund-result-timeout=1m",
        "saga.recovery.base-delay=5s",
        "saga.recovery.max-delay=1m",
        "saga.recovery.jitter-ratio=0",
        "saga.recovery.claim-lease=30s"
})
class SagaRecoveryIntegrationTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final Instant INITIAL_TIME = Instant.parse("2026-08-23T00:00:00Z");

    @Autowired private OrderIntegrationTestSupport fixtures;
    @Autowired private SagaTransactionService sagaTransactionService;
    @Autowired private SagaCompensationService compensationService;
    @Autowired private SagaRecoveryTransactionService recoveryTransactionService;
    @Autowired private PaymentSagaOrchestrator orchestrator;
    @Autowired private SagaRecoveryProperties properties;
    @Autowired private SagaInstanceRepository sagaRepository;
    @SpyBean private OutboxEventRepository outboxRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private PaymentClient paymentClient;
    @MockBean private Clock clock;

    private final AtomicReference<Instant> now = new AtomicReference<>(INITIAL_TIME);
    private final List<PaymentPendingFixture> createdFixtures = new ArrayList<>();
    private SagaRecoveryWorker worker;

    @BeforeEach
    void setUp() {
        now.set(INITIAL_TIME);
        when(clock.instant()).thenAnswer(invocation -> now.get());
        when(clock.getZone()).thenReturn(ZONE);
        worker = new SagaRecoveryWorker(recoveryTransactionService, orchestrator, properties);
    }

    @AfterEach
    void tearDown() {
        createdFixtures.forEach(fixtures::cleanup);
        createdFixtures.clear();
    }

    @Test
    void paymentUnknownRecoveryClaimsWithoutTransactionDuringRemoteCall_andConvergesApproved() {
        PaymentPendingFixture fixture = fixture();
        SagaSnapshot unknown = makeUnknownDue(fixture);
        Long orderId = fixture.order().getId();

        when(paymentClient.getStatus(orderId)).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            SagaInstance claimed = sagaRepository.findByOrderId(orderId).orElseThrow();
            assertThat(claimed.getStatus()).isEqualTo(SagaStatus.PAYMENT_UNKNOWN);
            assertThat(claimed.getNextRetryAt()).isAfter(localNow());
            return status(fixture, PaymentResultStatus.APPROVED, null);
        });

        worker.recoverDueSagas();

        SagaInstance completed = sagaRepository.findByOrderId(orderId).orElseThrow();
        assertThat(completed.getSagaId()).isEqualTo(unknown.sagaId());
        assertThat(completed.getStatus()).isEqualTo(SagaStatus.COMPLETED);
        assertThat(completed.getRetryCount()).isZero();
        assertThat(completed.getNextRetryAt()).isNull();
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(Status.PAID);
    }

    @Test
    void unresolvedPaymentRecoverySchedulesBackoffAndRecordsConciseError() {
        PaymentPendingFixture fixture = fixture();
        makeUnknownDue(fixture);
        Long orderId = fixture.order().getId();
        when(paymentClient.getStatus(orderId)).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            throw new CustomException(OrderErrorCode.PAYMENT_SERVICE_UNAVAILABLE);
        });

        worker.recoverDueSagas();

        SagaInstance retried = sagaRepository.findByOrderId(orderId).orElseThrow();
        assertThat(retried.getStatus()).isEqualTo(SagaStatus.PAYMENT_UNKNOWN);
        assertThat(retried.getRetryCount()).isEqualTo(1);
        assertThat(retried.getNextRetryAt()).isEqualTo(localNow().plusSeconds(5));
        assertThat(retried.getLastError()).contains("CustomException");
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
                .isEqualTo(Status.PAYMENT_PENDING);
        verify(paymentClient, never()).refund(anyLong(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void compensationWaitsForInitialDeadline_thenReissuesDistinctOutboxWithSameRefundKey() throws Exception {
        PaymentPendingFixture fixture = fixture();
        SagaSnapshot compensating = makeCompensating(fixture);
        Long orderId = fixture.order().getId();

        SagaInstance initial = sagaRepository.findById(compensating.sagaId()).orElseThrow();
        assertThat(initial.getNextRetryAt()).isEqualTo(localNow().plusMinutes(1));
        assertThat(initial.getRetryCount()).isZero();
        assertThat(paymentSagaOutbox(orderId)).hasSize(1);

        advanceSeconds(59);
        worker.recoverDueSagas();
        assertThat(paymentSagaOutbox(orderId)).hasSize(1);
        assertThat(sagaRepository.findById(compensating.sagaId()).orElseThrow().getRetryCount()).isZero();

        advanceSeconds(2);
        worker.recoverDueSagas();

        List<OutboxEvent> events = paymentSagaOutbox(orderId);
        assertThat(events).hasSize(2);
        assertThat(events).extracting(OutboxEvent::getEventId).doesNotHaveDuplicates();
        List<RefundPaymentCommand> commands = events.stream()
                .map(event -> readRefundCommand(event.getPayload()))
                .toList();
        assertThat(commands).extracting(RefundPaymentCommand::idempotencyKey)
                .containsOnly("REFUND:" + compensating.sagaId());
        SagaInstance retried = sagaRepository.findById(compensating.sagaId()).orElseThrow();
        assertThat(retried.getStatus()).isEqualTo(SagaStatus.COMPENSATING);
        assertThat(retried.getRetryCount()).isEqualTo(1);
        assertThat(retried.getNextRetryAt()).isEqualTo(localNow().plusMinutes(1));
    }

    @Test
    void thirdUnknownFailureAtomicallyReconcilesAndCreatesSingleAlert() {
        PaymentPendingFixture fixture = fixture();
        SagaSnapshot unknown = makeUnknownDue(fixture);
        setRecoveryFailure(unknown.sagaId(), 2, localNow().minusSeconds(1));
        when(paymentClient.getStatus(fixture.order().getId()))
                .thenThrow(new CustomException(OrderErrorCode.PAYMENT_SERVICE_UNAVAILABLE));

        worker.recoverDueSagas();
        worker.recoverDueSagas();

        SagaInstance reconciled = sagaRepository.findById(unknown.sagaId()).orElseThrow();
        assertThat(reconciled.getStatus()).isEqualTo(SagaStatus.RECONCILIATION_REQUIRED);
        assertThat(reconciled.getRetryCount()).isEqualTo(3);
        assertThat(reconciled.getNextRetryAt()).isNull();
        assertThat(paymentSagaOutbox(fixture.order().getId()).stream()
                .filter(this::isReconciliationAlert)).hasSize(1);
        verify(paymentClient, org.mockito.Mockito.times(1)).getStatus(fixture.order().getId());
    }

    @Test
    void thirdPendingNotFoundReconcilesInsteadOfCancellingOrRemainingForever() {
        PaymentPendingFixture fixture = fixture();
        SagaSnapshot started = sagaTransactionService.start(fixture.order().getId());
        SagaSnapshot pending = sagaTransactionService.transition(
                started.sagaId(), SagaStatus.PAYMENT_PENDING);
        setRecoveryFailure(pending.sagaId(), 2, localNow().minusSeconds(1));
        when(paymentClient.getStatus(fixture.order().getId()))
                .thenThrow(new CustomException(OrderErrorCode.PAYMENT_NOT_FOUND));

        worker.recoverDueSagas();

        SagaInstance reconciled = sagaRepository.findById(pending.sagaId()).orElseThrow();
        assertThat(reconciled.getStatus()).isEqualTo(SagaStatus.RECONCILIATION_REQUIRED);
        assertThat(reconciled.getRetryCount()).isEqualTo(3);
        assertThat(orderRepository.findById(fixture.order().getId()).orElseThrow().getStatus())
                .isEqualTo(Status.PAYMENT_PENDING);
        assertThat(paymentSagaOutbox(fixture.order().getId()).stream()
                .filter(this::isReconciliationAlert)).hasSize(1);
    }

    @Test
    void thirdCompensationFailureReconcilesWithoutAnotherRefundCommand() {
        PaymentPendingFixture fixture = fixture();
        SagaSnapshot compensating = makeCompensating(fixture);
        Long orderId = fixture.order().getId();
        setRecoveryFailure(compensating.sagaId(), 2, localNow().minusSeconds(1));

        worker.recoverDueSagas();
        worker.recoverDueSagas();

        SagaInstance reconciled = sagaRepository.findById(compensating.sagaId()).orElseThrow();
        assertThat(reconciled.getStatus()).isEqualTo(SagaStatus.RECONCILIATION_REQUIRED);
        assertThat(reconciled.getRetryCount()).isEqualTo(3);
        List<OutboxEvent> events = paymentSagaOutbox(orderId);
        assertThat(events.stream().filter(this::isRefundCommand)).hasSize(1);
        assertThat(events.stream().filter(this::isReconciliationAlert)).hasSize(1);
        verify(paymentClient, never()).getStatus(orderId);
    }

    @Test
    void reconciliationAlertSaveFailureRollsBackSagaTransitionAndRetryIncrement() {
        PaymentPendingFixture fixture = fixture();
        SagaSnapshot unknown = makeUnknownDue(fixture);
        Long orderId = fixture.order().getId();
        setRecoveryFailure(unknown.sagaId(), 2, localNow().minusSeconds(1));
        when(paymentClient.getStatus(orderId))
                .thenThrow(new CustomException(OrderErrorCode.PAYMENT_SERVICE_UNAVAILABLE));
        doThrow(new IllegalStateException("alert outbox unavailable"))
                .when(outboxRepository).save(org.mockito.ArgumentMatchers.argThat(this::isReconciliationAlert));

        org.assertj.core.api.Assertions.assertThatThrownBy(worker::recoverDueSagas)
                .isInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("alert outbox unavailable");

        SagaInstance rolledBack = sagaRepository.findById(unknown.sagaId()).orElseThrow();
        assertThat(rolledBack.getStatus()).isEqualTo(SagaStatus.PAYMENT_UNKNOWN);
        assertThat(rolledBack.getRetryCount()).isEqualTo(2);
        assertThat(paymentSagaOutbox(orderId).stream().filter(this::isReconciliationAlert)).isEmpty();
    }

    @Test
    void concurrentWorkersClaimOneSagaOnce_andExpiredLeaseBecomesDueAgain() throws Exception {
        PaymentPendingFixture fixture = fixture();
        SagaSnapshot unknown = makeUnknownDue(fixture);
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Optional<SagaRecoveryClaim>> first = pool.submit(() -> {
                start.await();
                return recoveryTransactionService.claimNext();
            });
            Future<Optional<SagaRecoveryClaim>> second = pool.submit(() -> {
                start.await();
                return recoveryTransactionService.claimNext();
            });
            start.countDown();

            List<Optional<SagaRecoveryClaim>> results = List.of(
                    first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertThat(results.stream().filter(Optional::isPresent)).hasSize(1);
            assertThat(recoveryTransactionService.claimNext()).isEmpty();

            advanceSeconds(31);
            SagaRecoveryClaim reclaimed = recoveryTransactionService.claimNext().orElseThrow();
            assertThat(reclaimed.sagaId()).isEqualTo(unknown.sagaId());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void nanosecondClockClaimUsesPersistedMicrosecondFenceAndAcceptsCurrentWorkerResult() {
        now.set(Instant.parse("2026-08-23T00:00:00.123456789Z"));
        PaymentPendingFixture fixture = fixture();
        SagaSnapshot unknown = makeUnknownDue(fixture);

        SagaRecoveryClaim claim = recoveryTransactionService.claimNext().orElseThrow();
        SagaInstance persisted = sagaRepository.findById(unknown.sagaId()).orElseThrow();

        assertThat(claim.claimedUntil().getNano() % 1_000).isZero();
        assertThat(persisted.getNextRetryAt()).isEqualTo(claim.claimedUntil());

        SagaRecoveryUpdateOutcome outcome = recoveryTransactionService.recordPaymentUnknownFailure(
                claim, new CustomException(OrderErrorCode.PAYMENT_SERVICE_UNAVAILABLE));

        assertThat(outcome).isEqualTo(SagaRecoveryUpdateOutcome.RETRY_SCHEDULED);
        assertThat(sagaRepository.findById(unknown.sagaId()).orElseThrow().getRetryCount()).isEqualTo(1);
    }

    @Test
    void expiredClaimRejectsLateWorkerAndAcceptsReclaimingWorker() {
        now.set(Instant.parse("2026-08-23T00:00:00.987654321Z"));
        PaymentPendingFixture fixture = fixture();
        SagaSnapshot unknown = makeUnknownDue(fixture);

        SagaRecoveryClaim workerA = recoveryTransactionService.claimNext().orElseThrow();
        advanceSeconds(31);
        SagaRecoveryClaim workerB = recoveryTransactionService.claimNext().orElseThrow();

        assertThat(workerB.claimedUntil()).isNotEqualTo(workerA.claimedUntil());
        assertThat(sagaRepository.findById(unknown.sagaId()).orElseThrow().getNextRetryAt())
                .isEqualTo(workerB.claimedUntil());

        SagaRecoveryUpdateOutcome lateA = recoveryTransactionService.recordPaymentUnknownFailure(
                workerA, new CustomException(OrderErrorCode.PAYMENT_SERVICE_UNAVAILABLE));
        assertThat(lateA).isEqualTo(SagaRecoveryUpdateOutcome.STALE_CLAIM);
        assertThat(sagaRepository.findById(unknown.sagaId()).orElseThrow().getRetryCount()).isZero();

        SagaRecoveryUpdateOutcome currentB = recoveryTransactionService.recordPaymentUnknownFailure(
                workerB, new CustomException(OrderErrorCode.PAYMENT_SERVICE_UNAVAILABLE));
        assertThat(currentB).isEqualTo(SagaRecoveryUpdateOutcome.RETRY_SCHEDULED);
        assertThat(sagaRepository.findById(unknown.sagaId()).orElseThrow().getRetryCount()).isEqualTo(1);
    }

    @Test
    void secondFailureStillSchedulesRetryInsteadOfReconciling() {
        PaymentPendingFixture fixture = fixture();
        SagaSnapshot unknown = makeUnknownDue(fixture);
        setRecoveryFailure(unknown.sagaId(), 1, localNow().minusSeconds(1));
        when(paymentClient.getStatus(fixture.order().getId()))
                .thenThrow(new CustomException(OrderErrorCode.PAYMENT_SERVICE_UNAVAILABLE));

        worker.recoverDueSagas();

        SagaInstance saga = sagaRepository.findById(unknown.sagaId()).orElseThrow();
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.PAYMENT_UNKNOWN);
        assertThat(saga.getRetryCount()).isEqualTo(2);
        assertThat(saga.getNextRetryAt()).isEqualTo(localNow().plusSeconds(10));
    }

    private PaymentPendingFixture fixture() {
        PaymentPendingFixture fixture = fixtures.createPaymentPendingOrder();
        createdFixtures.add(fixture);
        return fixture;
    }

    private SagaSnapshot makeUnknownDue(PaymentPendingFixture fixture) {
        SagaSnapshot started = sagaTransactionService.start(fixture.order().getId());
        sagaTransactionService.transition(started.sagaId(), SagaStatus.PAYMENT_PENDING);
        SagaSnapshot unknown = sagaTransactionService.markPaymentUnknown(started.sagaId(), "initial unknown");
        setRecoveryFailure(unknown.sagaId(), 0, localNow().minusSeconds(1));
        return unknown;
    }

    private SagaSnapshot makeCompensating(PaymentPendingFixture fixture) {
        SagaSnapshot started = sagaTransactionService.start(fixture.order().getId());
        sagaTransactionService.transition(started.sagaId(), SagaStatus.PAYMENT_PENDING);
        sagaTransactionService.transition(started.sagaId(), SagaStatus.PAYMENT_COMPLETED);
        compensationService.start(started.sagaId(), fixture.order().getId(), "completion failed");
        SagaInstance compensating = sagaRepository.findById(started.sagaId()).orElseThrow();
        return new SagaSnapshot(
                compensating.getSagaId(), compensating.getOrderId(),
                compensating.getStatus(), compensating.getCurrentStep());
    }

    private void setRecoveryFailure(java.util.UUID sagaId, int count, LocalDateTime dueAt) {
        transactionTemplate.executeWithoutResult(ignored -> sagaRepository.findById(sagaId).orElseThrow()
                .recordRecoveryFailure(count, dueAt, "test unresolved"));
    }

    private List<OutboxEvent> paymentSagaOutbox(Long orderId) {
        return outboxRepository.findAll().stream()
                .filter(event -> "PAYMENT_SAGA".equals(event.getAggregateType()))
                .filter(event -> orderId.equals(event.getAggregateId()))
                .toList();
    }

    private RefundPaymentCommand readRefundCommand(String payload) {
        try {
            return objectMapper.readValue(payload, RefundPaymentCommand.class);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private boolean isReconciliationAlert(OutboxEvent event) {
        try {
            return SagaReconciliationAlert.EVENT_TYPE.equals(
                    objectMapper.readTree(event.getPayload()).path("eventType").asText());
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private boolean isRefundCommand(OutboxEvent event) {
        try {
            return RefundPaymentCommand.EVENT_TYPE.equals(
                    objectMapper.readTree(event.getPayload()).path("eventType").asText());
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private PaymentStatusResponse status(
            PaymentPendingFixture fixture,
            PaymentResultStatus status,
            String failureReason
    ) {
        return new PaymentStatusResponse(
                fixture.order().getId(), status, fixture.totalPrice(), fixture.totalPrice(),
                status == PaymentResultStatus.APPROVED ? localNow() : null,
                "recovery-payment-key", failureReason, null, null, null);
    }

    private LocalDateTime localNow() {
        return LocalDateTime.ofInstant(now.get(), ZONE);
    }

    private void advanceSeconds(long seconds) {
        now.updateAndGet(value -> value.plusSeconds(seconds));
    }
}
