package com.example.fan_cafe.order.saga.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.payment.client.PaymentClient;
import com.example.fan_cafe.order.payment.client.PaymentResultStatus;
import com.example.fan_cafe.order.payment.client.PaymentStatusResponse;
import com.example.fan_cafe.order.saga.domain.SagaInstance;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import com.example.fan_cafe.order.saga.exception.SagaErrorCode;
import com.example.fan_cafe.order.saga.infrastructure.SagaInstanceRepository;
import com.example.fan_cafe.order.saga.interfaces.dto.SagaAdminResponse;
import com.example.fan_cafe.order.saga.interfaces.dto.SagaManualActionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SagaAdminService {
    private static final String MANUAL_COMPENSATION_REASON = "manual saga compensation";

    private final SagaInstanceRepository sagaRepository;
    private final SagaTransactionService sagaTransactionService;
    private final PaymentSagaOrchestrator paymentSagaOrchestrator;
    private final SagaCompensationService compensationService;
    private final PaymentClient paymentClient;

    @Transactional(readOnly = true)
    public List<SagaAdminResponse> getReconciliationRequiredSagas() {
        return sagaRepository.findAllByStatusOrderByUpdatedAtDesc(
                        SagaStatus.RECONCILIATION_REQUIRED).stream()
                .map(SagaAdminResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SagaAdminResponse getSaga(UUID sagaId) {
        return SagaAdminResponse.from(findSaga(sagaId));
    }

    public SagaManualActionResponse requestAction(UUID sagaId, SagaAdminAction action) {
        SagaSnapshot snapshot = sagaTransactionService.get(sagaId);
        log.info("[SAGA ADMIN] manual action requested sagaId={}, status={}, action={}",
                sagaId, snapshot.status(), action);
        return switch (action) {
            case RECHECK_PAYMENT_STATUS -> recheckPaymentStatus(snapshot);
            case RESUME_FORWARD -> resumeForward(snapshot);
            case COMPENSATE -> compensate(snapshot);
        };
    }

    private SagaManualActionResponse recheckPaymentStatus(SagaSnapshot snapshot) {
        if (snapshot.status() == SagaStatus.PAYMENT_UNKNOWN) {
            paymentSagaOrchestrator.recoverPaymentUnknown(snapshot.sagaId(), snapshot.orderId());
            return response(snapshot.sagaId(), null);
        }
        if (snapshot.status() == SagaStatus.RECONCILIATION_REQUIRED) {
            PaymentStatusResponse payment = paymentClient.getStatus(snapshot.orderId());
            return response(snapshot.sagaId(), payment.status());
        }
        if (isTerminalOrProgressed(snapshot.status())) {
            return response(snapshot.sagaId(), null);
        }
        throw new CustomException(SagaErrorCode.INVALID_MANUAL_ACTION);
    }

    private SagaManualActionResponse resumeForward(SagaSnapshot snapshot) {
        if (snapshot.status() == SagaStatus.COMPLETED) {
            return response(snapshot.sagaId(), PaymentResultStatus.APPROVED);
        }

        PaymentStatusResponse observedPayment = null;
        if (snapshot.status() == SagaStatus.RECONCILIATION_REQUIRED) {
            observedPayment = requireApprovedPayment(snapshot.orderId());
        } else if (snapshot.status() != SagaStatus.PAYMENT_COMPLETED) {
            throw new CustomException(SagaErrorCode.INVALID_MANUAL_ACTION);
        }

        sagaTransactionService.prepareManualForward(snapshot.sagaId());
        if (observedPayment == null) {
            paymentSagaOrchestrator.resumePaymentCompleted(snapshot.sagaId(), snapshot.orderId());
        } else {
            paymentSagaOrchestrator.resumePaymentCompleted(
                    snapshot.sagaId(), snapshot.orderId(), observedPayment.approvedAt());
        }
        return response(snapshot.sagaId(),
                observedPayment == null ? null : observedPayment.status());
    }

    private SagaManualActionResponse compensate(SagaSnapshot snapshot) {
        if (snapshot.status() == SagaStatus.COMPENSATED) {
            return response(snapshot.sagaId(), PaymentResultStatus.REFUNDED);
        }

        PaymentResultStatus observedStatus = null;
        if (snapshot.status() == SagaStatus.RECONCILIATION_REQUIRED) {
            PaymentStatusResponse payment = paymentClient.getStatus(snapshot.orderId());
            if (payment.status() != PaymentResultStatus.APPROVED
                    && payment.status() != PaymentResultStatus.REFUNDED) {
                throw new CustomException(SagaErrorCode.PAYMENT_NOT_APPROVED_FOR_MANUAL_ACTION);
            }
            observedStatus = payment.status();
        } else if (snapshot.status() != SagaStatus.PAYMENT_COMPLETED
                && snapshot.status() != SagaStatus.COMPENSATING) {
            throw new CustomException(SagaErrorCode.INVALID_MANUAL_ACTION);
        }

        compensationService.requestManual(
                snapshot.sagaId(), snapshot.orderId(), MANUAL_COMPENSATION_REASON);
        return response(snapshot.sagaId(), observedStatus);
    }

    private PaymentStatusResponse requireApprovedPayment(Long orderId) {
        PaymentStatusResponse payment = paymentClient.getStatus(orderId);
        if (payment.status() != PaymentResultStatus.APPROVED || payment.approvedAt() == null) {
            throw new CustomException(SagaErrorCode.PAYMENT_NOT_APPROVED_FOR_MANUAL_ACTION);
        }
        return payment;
    }

    private SagaManualActionResponse response(UUID sagaId, PaymentResultStatus paymentStatus) {
        return new SagaManualActionResponse(getSaga(sagaId), paymentStatus);
    }

    private SagaInstance findSaga(UUID sagaId) {
        return sagaRepository.findById(sagaId)
                .orElseThrow(() -> new CustomException(SagaErrorCode.SAGA_NOT_FOUND));
    }

    private boolean isTerminalOrProgressed(SagaStatus status) {
        return status == SagaStatus.PAYMENT_COMPLETED
                || status == SagaStatus.COMPLETED
                || status == SagaStatus.CANCELLED
                || status == SagaStatus.COMPENSATING
                || status == SagaStatus.COMPENSATED;
    }
}
