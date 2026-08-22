package com.example.fan_cafe.order.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import com.example.fan_cafe.order.interfaces.dto.OrderQueryResponse;
import com.example.fan_cafe.order.payment.client.PaymentResultResponse;
import com.example.fan_cafe.order.payment.client.PaymentResultStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPaymentResultServiceTest {
    @Mock private OrderPaymentCommandService commandService;
    @InjectMocks private OrderPaymentResultService resultService;

    @Test
    void approvedResult_appliesOrderPaid() {
        PaymentResultResponse result = new PaymentResultResponse(
                10L, PaymentResultStatus.APPROVED, "key", null, null);
        OrderQueryResponse response = mock(OrderQueryResponse.class);
        when(commandService.applyPaymentApproved(10L, "approved")).thenReturn(response);

        resultService.apply(10L, result, "approved", "failed");

        verify(commandService).applyPaymentApproved(10L, "approved");
        verify(commandService, never()).applyPaymentFailed(anyLong(), anyString());
    }

    @Test
    void amountMismatch_appliesFailedBeforeReturningExistingError() {
        PaymentResultResponse result = new PaymentResultResponse(
                10L, PaymentResultStatus.FAILED, null,
                "approval amount mismatch", "PAYMENT_AMOUNT_MISMATCH");
        when(commandService.applyPaymentFailed(10L, "approval amount mismatch"))
                .thenReturn(mock(OrderQueryResponse.class));

        assertThatThrownBy(() -> resultService.apply(10L, result, "approved", "failed"))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(OrderErrorCode.PAYMENT_AMOUNT_MISMATCH);

        verify(commandService).applyPaymentFailed(10L, "approval amount mismatch");
    }

    @Test
    void mismatchedOrderId_isRejected() {
        PaymentResultResponse result = new PaymentResultResponse(
                11L, PaymentResultStatus.APPROVED, "key", null, null);

        assertThatThrownBy(() -> resultService.apply(10L, result, "approved", "failed"))
                .isInstanceOf(CustomException.class);
        verifyNoInteractions(commandService);
    }
}
