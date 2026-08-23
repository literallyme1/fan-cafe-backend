package com.example.fan_cafe.order.saga.recovery;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.exception.GlobalErrorCode;
import com.example.fan_cafe.order.saga.domain.PaymentSagaStateMachine;
import com.example.fan_cafe.order.saga.domain.SagaInstance;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import com.example.fan_cafe.order.saga.exception.SagaErrorCode;
import com.example.fan_cafe.order.saga.infrastructure.SagaInstanceRepository;
import com.example.fan_cafe.order.saga.messaging.RefundPaymentCommand;
import com.example.fan_cafe.outbox.domain.OutboxEvent;
import com.example.fan_cafe.outbox.infrastructure.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SagaRecoveryTransactionService {
    private static final String AGGREGATE_TYPE = "PAYMENT_SAGA";
    private static final int MAX_ERROR_LENGTH = 1000;

    private final SagaInstanceRepository sagaRepository;
    private final OutboxEventRepository outboxRepository;
    private final PaymentSagaStateMachine stateMachine;
    private final SagaRecoveryProperties properties;
    private final SagaRecoveryBackoffPolicy backoffPolicy;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional
    public Optional<SagaRecoveryClaim> claimNext() {
        LocalDateTime now = LocalDateTime.now(clock);
        return sagaRepository.findNextDueRecoveryForUpdateSkipLocked(now)
                .map(saga -> {
                    LocalDateTime claimedUntil = now.plus(properties.getClaimLease());
                    saga.claimRecoveryUntil(claimedUntil);
                    return SagaRecoveryClaim.from(saga, claimedUntil);
                });
    }

    @Transactional
    public SagaRecoveryUpdateOutcome recordPaymentUnknownFailure(
            SagaRecoveryClaim claim,
            RuntimeException failure
    ) {
        SagaInstance saga = findClaimedSaga(claim);
        if (saga == null) {
            return SagaRecoveryUpdateOutcome.STALE_CLAIM;
        }
        return recordFailureOrReconcile(saga, claim.status(), summarize(failure));
    }

    @Transactional
    public SagaRecoveryUpdateOutcome retryCompensation(SagaRecoveryClaim claim) {
        SagaInstance saga = findClaimedSaga(claim);
        if (saga == null) {
            return SagaRecoveryUpdateOutcome.STALE_CLAIM;
        }

        int nextFailureCount = saga.getRetryCount() + 1;
        String errorSummary = "Refund result deadline elapsed without COMPENSATED result";
        if (nextFailureCount >= properties.getMaxRetryCount()) {
            return reconcile(saga, claim.status(), nextFailureCount, errorSummary);
        }

        RefundPaymentCommand command = RefundPaymentCommand.of(
                saga.getSagaId(), saga.getOrderId(), "saga compensation recovery");
        persistOutbox(OutboxEvent.init(
                AGGREGATE_TYPE, saga.getOrderId(), serialize(command)));

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime refundDeadline = now.plus(properties.getRefundResultTimeout());
        LocalDateTime backedOff = backoffPolicy.nextRetryAt(nextFailureCount);
        saga.recordRecoveryFailure(
                nextFailureCount,
                refundDeadline.isAfter(backedOff) ? refundDeadline : backedOff,
                errorSummary);
        return SagaRecoveryUpdateOutcome.RETRY_SCHEDULED;
    }

    private SagaRecoveryUpdateOutcome recordFailureOrReconcile(
            SagaInstance saga,
            SagaStatus previousStatus,
            String errorSummary
    ) {
        int nextFailureCount = saga.getRetryCount() + 1;
        if (nextFailureCount >= properties.getMaxRetryCount()) {
            return reconcile(saga, previousStatus, nextFailureCount, errorSummary);
        }
        saga.recordRecoveryFailure(
                nextFailureCount, backoffPolicy.nextRetryAt(nextFailureCount), errorSummary);
        return SagaRecoveryUpdateOutcome.RETRY_SCHEDULED;
    }

    private SagaRecoveryUpdateOutcome reconcile(
            SagaInstance saga,
            SagaStatus previousStatus,
            int failureCount,
            String errorSummary
    ) {
        stateMachine.transition(saga, SagaStatus.RECONCILIATION_REQUIRED);
        saga.recordReconciliationFailure(failureCount, errorSummary);
        SagaReconciliationAlert alert = SagaReconciliationAlert.of(
                saga.getSagaId(), saga.getOrderId(), previousStatus, failureCount, errorSummary);
        persistOutbox(OutboxEvent.init(
                AGGREGATE_TYPE, saga.getOrderId(), serialize(alert)));
        return SagaRecoveryUpdateOutcome.RECONCILIATION_REQUIRED;
    }

    private SagaInstance findClaimedSaga(SagaRecoveryClaim claim) {
        SagaInstance saga = sagaRepository.findBySagaIdForUpdate(claim.sagaId())
                .orElseThrow(() -> new CustomException(SagaErrorCode.SAGA_NOT_FOUND));
        return saga.hasClaim(claim.status(), claim.claimedUntil()) ? saga : null;
    }

    private String summarize(RuntimeException failure) {
        String message = failure.getMessage() == null ? "unknown recovery failure" : failure.getMessage();
        String summary = failure.getClass().getSimpleName() + ": "
                + message.replace('\r', ' ').replace('\n', ' ');
        return summary.length() <= MAX_ERROR_LENGTH
                ? summary
                : summary.substring(0, MAX_ERROR_LENGTH);
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException serializationFailure) {
            throw new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void persistOutbox(OutboxEvent event) {
        OutboxEvent saved = outboxRepository.save(event);
        outboxRepository.flush();
        saved.assignEventIdFromPrimaryKey();
        outboxRepository.save(saved);
    }
}
