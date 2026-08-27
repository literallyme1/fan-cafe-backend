package com.example.fan_cafe.order.saga.recovery;

import com.example.fan_cafe.order.saga.application.PaymentSagaOrchestrator;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class SagaRecoveryWorker {
    private final SagaRecoveryTransactionService recoveryTransactionService;
    private final PaymentSagaOrchestrator paymentSagaOrchestrator;
    private final SagaRecoveryProperties properties;
    private final List<SagaRecoveryClaimObserver> claimObservers;

    @Autowired
    public SagaRecoveryWorker(
            SagaRecoveryTransactionService recoveryTransactionService,
            PaymentSagaOrchestrator paymentSagaOrchestrator,
            SagaRecoveryProperties properties,
            List<SagaRecoveryClaimObserver> claimObservers
    ) {
        this.recoveryTransactionService = recoveryTransactionService;
        this.paymentSagaOrchestrator = paymentSagaOrchestrator;
        this.properties = properties;
        this.claimObservers = claimObservers;
    }

    SagaRecoveryWorker(
            SagaRecoveryTransactionService recoveryTransactionService,
            PaymentSagaOrchestrator paymentSagaOrchestrator,
            SagaRecoveryProperties properties
    ) {
        this(recoveryTransactionService, paymentSagaOrchestrator, properties, List.of());
    }

    public void recoverDueSagas() {
        for (int processed = 0; processed < properties.getBatchSize(); processed++) {
            Optional<SagaRecoveryClaim> claimed = recoveryTransactionService.claimNext();
            if (claimed.isEmpty()) {
                return;
            }
            claimObservers.forEach(observer -> observer.claimSucceeded(claimed.get()));
            process(claimed.get());
        }
    }

    private void process(SagaRecoveryClaim claim) {
        if (claim.status() == SagaStatus.COMPENSATING) {
            retryCompensation(claim);
            return;
        }
        if (claim.status() == SagaStatus.PAYMENT_PENDING) {
            recoverPaymentPending(claim);
            return;
        }
        if (claim.status() == SagaStatus.PAYMENT_COMPLETED) {
            recoverPaymentCompleted(claim);
            return;
        }
        recoverPaymentUnknown(claim);
    }

    private void recoverPaymentCompleted(SagaRecoveryClaim claim) {
        try {
            paymentSagaOrchestrator.resumePaymentCompleted(claim.sagaId(), claim.orderId());
        } catch (RuntimeException failure) {
            recoveryTransactionService.recordPaymentUnknownFailure(claim, failure);
            log.warn("[SAGA RECOVERY] order completion unresolved sagaId={}, retryCount={}",
                    claim.sagaId(), claim.retryCount(), failure);
        }
    }

    private void recoverPaymentPending(SagaRecoveryClaim claim) {
        try {
            paymentSagaOrchestrator.recoverPaymentPending(claim.sagaId(), claim.orderId());
        } catch (RuntimeException failure) {
            recoveryTransactionService.recordPaymentUnknownFailure(claim, failure);
            log.warn("[SAGA RECOVERY] pending payment unresolved sagaId={}, retryCount={}",
                    claim.sagaId(), claim.retryCount(), failure);
        }
    }

    private void recoverPaymentUnknown(SagaRecoveryClaim claim) {
        try {
            paymentSagaOrchestrator.recoverPaymentUnknown(claim.sagaId(), claim.orderId());
        } catch (RuntimeException failure) {
            recoveryTransactionService.recordPaymentUnknownFailure(claim, failure);
            log.warn("[SAGA RECOVERY] payment status unresolved sagaId={}, retryCount={}",
                    claim.sagaId(), claim.retryCount(), failure);
        }
    }

    private void retryCompensation(SagaRecoveryClaim claim) {
        try {
            recoveryTransactionService.retryCompensation(claim);
        } catch (RuntimeException failure) {
            log.error("[SAGA RECOVERY] refund command reissue failed sagaId={}",
                    claim.sagaId(), failure);
        }
    }
}
