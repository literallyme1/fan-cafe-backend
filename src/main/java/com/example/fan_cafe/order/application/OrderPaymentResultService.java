package com.example.fan_cafe.order.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import com.example.fan_cafe.order.interfaces.dto.OrderQueryResponse;
import com.example.fan_cafe.order.payment.client.PaymentResultResponse;
import com.example.fan_cafe.order.payment.client.PaymentResultStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderPaymentResultService {
    private final OrderPaymentCommandService commandService;

    public OrderQueryResponse apply(
            Long expectedOrderId,
            PaymentResultResponse result,
            String approvedReason,
            String defaultFailureReason
    ) {
        if (result.orderId() == null || !result.orderId().equals(expectedOrderId)) {
            throw new CustomException(OrderErrorCode.PAYMENT_SERVICE_ERROR);
        }
        if (result.status() == PaymentResultStatus.APPROVED) {
            return commandService.applyPaymentApproved(expectedOrderId, approvedReason);
        }
        if (result.status() == PaymentResultStatus.FAILED) {
            String reason = result.failureReason() == null ? defaultFailureReason : result.failureReason();
            OrderQueryResponse updated = commandService.applyPaymentFailed(expectedOrderId, reason);
            if ("PAYMENT_AMOUNT_MISMATCH".equals(result.failureCode())) {
                throw new CustomException(OrderErrorCode.PAYMENT_AMOUNT_MISMATCH);
            }
            return updated;
        }
        throw new CustomException(OrderErrorCode.PAYMENT_SERVICE_ERROR);
    }
}
