package com.example.payment.messaging;

import com.example.payment.application.PaymentService;
import com.example.payment.domain.PaymentStatus;
import com.example.payment.interfaces.dto.PaymentStatusResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentRefundCommandConsumerTest {
    @Mock private PaymentService paymentService;
    @Mock private PaymentRefundResultPublisher resultPublisher;

    @Test
    void duplicateCommandReusesPaymentRefundWithSameSagaId() {
        UUID sagaId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        RefundPaymentCommand command = new RefundPaymentCommand(
                RefundPaymentCommand.EVENT_TYPE,
                sagaId,
                10L,
                "order completion failed",
                "REFUND:" + sagaId
        );
        PaymentStatusResponse refunded = new PaymentStatusResponse(
                10L, PaymentStatus.REFUNDED, null, null, "pay-1", null,
                "REFUND:" + sagaId, "order completion failed", null);
        when(paymentService.refund(10L, sagaId, "order completion failed"))
                .thenReturn(refunded);
        PaymentRefundCommandConsumer consumer = new PaymentRefundCommandConsumer(
                paymentService, resultPublisher);

        consumer.consume(command);
        consumer.consume(command);

        verify(paymentService, times(2)).refund(10L, sagaId, "order completion failed");
        ArgumentCaptor<PaymentRefundedResult> resultCaptor =
                ArgumentCaptor.forClass(PaymentRefundedResult.class);
        verify(resultPublisher, times(2)).publish(resultCaptor.capture());
        assertThat(resultCaptor.getAllValues())
                .allSatisfy(result -> {
                    assertThat(result.sagaId()).isEqualTo(sagaId);
                    assertThat(result.status()).isEqualTo(PaymentStatus.REFUNDED);
                    assertThat(result.refundIdempotencyKey()).isEqualTo("REFUND:" + sagaId);
                });
    }
}
