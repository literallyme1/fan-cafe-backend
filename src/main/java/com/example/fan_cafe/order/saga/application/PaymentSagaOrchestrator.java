package com.example.fan_cafe.order.saga.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.application.OrderPaymentResultService;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import com.example.fan_cafe.order.interfaces.dto.OrderQueryResponse;
import com.example.fan_cafe.order.payment.client.PaymentClient;
import com.example.fan_cafe.order.payment.client.PaymentResultResponse;
import com.example.fan_cafe.order.payment.client.PaymentResultStatus;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentSagaOrchestrator {
    private static final String APPROVED_REASON = "mock payment approved";
    private static final String FAILED_REASON = "mock payment failed";

    private final SagaTransactionService sagaTransactionService;
    private final SagaOrderCompletionService completionService;
    private final PaymentClient paymentClient;
    private final OrderPaymentResultService orderPaymentResultService;

    public OrderQueryResponse approve(
            Long orderId,
            BigDecimal expectedAmount,
            BigDecimal approvalAmount,
        String paymentKey
    ) {
        SagaSnapshot saga = sagaTransactionService.start(orderId);
        saga = sagaTransactionService.advanceToMilestone(saga.sagaId(), SagaStatus.PAYMENT_PENDING);
        if (saga.status().isAtOrAfter(SagaStatus.PAYMENT_COMPLETED)) {
            return completionService.complete(saga.sagaId(), orderId, APPROVED_REASON);
        }

        PaymentResultResponse payment = paymentClient.approve(
                orderId, expectedAmount, approvalAmount, paymentKey);
        if (payment.status() != PaymentResultStatus.APPROVED) {
            return orderPaymentResultService.apply(orderId, payment, APPROVED_REASON, FAILED_REASON);
        }
        if (!orderId.equals(payment.orderId())) {
            throw new CustomException(OrderErrorCode.PAYMENT_SERVICE_ERROR);
        }

        sagaTransactionService.advanceToMilestone(saga.sagaId(), SagaStatus.PAYMENT_COMPLETED);
        return completionService.complete(saga.sagaId(), orderId, APPROVED_REASON);
    }
}
