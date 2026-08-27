package com.example.payment.messaging;

import com.example.payment.application.PaymentService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PaymentApprovalCommandConsumerTest {
    @Test
    void fastPathAndDuplicateCommandShareSameIdempotentApprovalMethod() {
        PaymentService paymentService = mock(PaymentService.class);
        PaymentApprovalCommandConsumer consumer = new PaymentApprovalCommandConsumer(paymentService);
        ApprovePaymentCommand command = new ApprovePaymentCommand(
                ApprovePaymentCommand.EVENT_TYPE,
                10L,
                new BigDecimal("10000"),
                new BigDecimal("10000"),
                "campaign-key");

        consumer.consume(command);
        consumer.consume(command);

        verify(paymentService, times(2)).approve(
                10L, new BigDecimal("10000"), new BigDecimal("10000"), "campaign-key");
    }
}
