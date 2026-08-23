package com.example.fan_cafe.order.saga.recovery;

import com.example.fan_cafe.order.saga.application.PaymentSagaOrchestrator;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "saga.recovery.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SagaRecoveryWorker {
    private final SagaRecoveryTransactionService recoveryTransactionService;
    private final PaymentSagaOrchestrator paymentSagaOrchestrator;
    private final SagaRecoveryProperties properties;

    @Scheduled(fixedDelayString = "${saga.recovery.fixed-delay:5s}")
    public void recoverDueSagas() {
        for (int processed = 0; processed < properties.getBatchSize(); processed++) {
            Optional<SagaRecoveryClaim> claimed = recoveryTransactionService.claimNext();
            if (claimed.isEmpty()) {
                return;
            }
            process(claimed.get());
        }
    }

    private void process(SagaRecoveryClaim claim) {
        if (claim.status() == SagaStatus.COMPENSATING) {
            retryCompensation(claim);
            return;
        }
        recoverPaymentUnknown(claim);
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
