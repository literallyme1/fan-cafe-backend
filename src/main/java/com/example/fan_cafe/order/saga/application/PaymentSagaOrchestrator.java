package com.example.fan_cafe.order.saga.application;

import com.example.fan_cafe.campaign.exception.CampaignApprovalAfterDeadlineException;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import com.example.fan_cafe.order.interfaces.dto.OrderQueryResponse;
import com.example.fan_cafe.order.payment.client.PaymentClient;
import com.example.fan_cafe.order.payment.client.PaymentOutcomeUnknownException;
import com.example.fan_cafe.order.payment.client.PaymentResultResponse;
import com.example.fan_cafe.order.payment.client.PaymentResultStatus;
import com.example.fan_cafe.order.payment.client.PaymentStatusResponse;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import com.example.fan_cafe.order.saga.exception.OrderCompletionFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentSagaOrchestrator {
    private static final String APPROVED_REASON = "mock payment approved";
    private static final String FAILED_REASON = "mock payment failed";

    private final SagaTransactionService sagaTransactionService;
    private final SagaOrderCompletionService completionService;
    private final SagaCompensationService compensationService;
    private final SagaPaymentFailureService paymentFailureService;
    private final PaymentClient paymentClient;

    public OrderQueryResponse approve(
            Long orderId,
            BigDecimal expectedAmount,
            BigDecimal approvalAmount,
        String paymentKey
    ) {
        SagaSnapshot saga = sagaTransactionService.start(orderId);
        return switch (saga.status()) {
            case STARTED -> approvePending(
                    sagaTransactionService.transition(saga.sagaId(), SagaStatus.PAYMENT_PENDING),
                    orderId, expectedAmount, approvalAmount, paymentKey);
            case PAYMENT_PENDING -> approvePending(
                    saga, orderId, expectedAmount, approvalAmount, paymentKey);
            case PAYMENT_UNKNOWN -> resolveUnknownPayment(saga, orderId);
            case PAYMENT_COMPLETED, COMPLETED -> completeOrder(saga.sagaId(), orderId, null);
            case CANCELLED -> paymentFailureService.fail(
                    saga.sagaId(), orderId, FAILED_REASON);
            case COMPENSATING, COMPENSATED, RECONCILIATION_REQUIRED ->
                    throw new CustomException(OrderErrorCode.INVALID_PAYMENT_STATE);
        };
    }

    public OrderQueryResponse recoverPaymentUnknown(UUID sagaId, Long orderId) {
        return resolveUnknownPayment(
                new SagaSnapshot(
                        sagaId, orderId, SagaStatus.PAYMENT_UNKNOWN,
                        com.example.fan_cafe.order.saga.domain.SagaStep.PAYMENT_STATUS_CHECK),
                orderId);
    }

    public OrderQueryResponse recoverPaymentPending(UUID sagaId, Long orderId) {
        try {
            return resolveUnknownPayment(
                    new SagaSnapshot(
                            sagaId, orderId, SagaStatus.PAYMENT_PENDING,
                            com.example.fan_cafe.order.saga.domain.SagaStep.PAYMENT_STATUS_CHECK),
                    orderId);
        } catch (CustomException failure) {
            if (failure.getErrorCode() == OrderErrorCode.PAYMENT_NOT_FOUND) {
                return paymentFailureService.fail(
                        sagaId, orderId, "payment execute was not observed before recovery deadline");
            }
            throw failure;
        }
    }

    public OrderQueryResponse resumePaymentCompleted(UUID sagaId, Long orderId) {
        return completeOrder(sagaId, orderId, null);
    }

    private OrderQueryResponse approvePending(
            SagaSnapshot saga,
            Long orderId,
            BigDecimal expectedAmount,
            BigDecimal approvalAmount,
            String paymentKey
    ) {
        PaymentResultResponse payment;
        try {
            payment = paymentClient.approve(orderId, expectedAmount, approvalAmount, paymentKey);
        } catch (PaymentOutcomeUnknownException unknown) {
            SagaSnapshot current = sagaTransactionService.markPaymentUnknown(
                    saga.sagaId(), summarizeUnknown(unknown));
            return continueAfterUnknown(current, orderId, unknown);
        }

        validatePaymentOrder(orderId, payment.orderId());
        if (payment.status() == PaymentResultStatus.FAILED) {
            String reason = payment.failureReason() == null ? FAILED_REASON : payment.failureReason();
            OrderQueryResponse response = paymentFailureService.fail(saga.sagaId(), orderId, reason);
            if ("PAYMENT_AMOUNT_MISMATCH".equals(payment.failureCode())) {
                throw new CustomException(OrderErrorCode.PAYMENT_AMOUNT_MISMATCH);
            }
            return response;
        }
        if (payment.status() != PaymentResultStatus.APPROVED) {
            throw new CustomException(OrderErrorCode.PAYMENT_SERVICE_ERROR);
        }

        sagaTransactionService.advanceToMilestone(saga.sagaId(), SagaStatus.PAYMENT_COMPLETED);
        return completeOrder(saga.sagaId(), orderId, payment.approvedAt());
    }

    private OrderQueryResponse continueAfterUnknown(
            SagaSnapshot saga,
            Long orderId,
            PaymentOutcomeUnknownException originalFailure
    ) {
        return switch (saga.status()) {
            case PAYMENT_UNKNOWN -> resolveUnknownPayment(saga, orderId);
            case PAYMENT_COMPLETED, COMPLETED -> completeOrder(saga.sagaId(), orderId, null);
            case CANCELLED -> paymentFailureService.fail(saga.sagaId(), orderId, FAILED_REASON);
            case STARTED, PAYMENT_PENDING, COMPENSATING, COMPENSATED,
                    RECONCILIATION_REQUIRED -> throw originalFailure;
        };
    }

    private OrderQueryResponse resolveUnknownPayment(SagaSnapshot saga, Long orderId) {
        PaymentStatusResponse payment = paymentClient.getStatus(orderId);
        validatePaymentOrder(orderId, payment.orderId());
        if (payment.status() == null) {
            throw new CustomException(OrderErrorCode.PAYMENT_SERVICE_ERROR);
        }

        return switch (payment.status()) {
            case APPROVED -> {
                var compensation = compensationService.startLateSuccessIfOrderCannotComplete(
                        saga.sagaId(), orderId);
                if (compensation.isPresent()) {
                    yield compensation.get();
                }
                sagaTransactionService.advanceToMilestone(
                        saga.sagaId(), SagaStatus.PAYMENT_COMPLETED);
                yield completeOrder(saga.sagaId(), orderId, payment.approvedAt());
            }
            case FAILED -> paymentFailureService.fail(
                    saga.sagaId(), orderId,
                    payment.failureReason() == null ? FAILED_REASON : payment.failureReason());
            case PENDING, REFUNDED ->
                    throw new CustomException(OrderErrorCode.PAYMENT_SERVICE_ERROR);
        };
    }

    private void validatePaymentOrder(Long expectedOrderId, Long actualOrderId) {
        if (!expectedOrderId.equals(actualOrderId)) {
            throw new CustomException(OrderErrorCode.PAYMENT_SERVICE_ERROR);
        }
    }

    private String summarizeUnknown(PaymentOutcomeUnknownException unknown) {
        return unknown.getClass().getSimpleName() + ": " + unknown.getErrorMessage();
    }

    private OrderQueryResponse completeOrder(UUID sagaId, Long orderId, LocalDateTime approvedAt) {
        try {
            return approvedAt == null
                    ? completionService.complete(sagaId, orderId, APPROVED_REASON)
                    : completionService.complete(sagaId, orderId, APPROVED_REASON, approvedAt);
        } catch (CampaignApprovalAfterDeadlineException lateApproval) {
            return compensationService.startCampaign(
                    sagaId,
                    orderId,
                    com.example.fan_cafe.order.saga.domain.SagaCompensationReason
                            .LATE_APPROVAL_AFTER_DEADLINE.name(),
                    lateApproval.getApprovedAt());
        } catch (OrderCompletionFailedException completionFailure) {
            compensationService.startAfterOrderCompletionFailure(
                    sagaId, orderId,
                    com.example.fan_cafe.order.saga.domain.SagaCompensationReason.ORDER_FAILURE.name(),
                    approvedAt);
            throw completionFailure;
        }
    }
}
