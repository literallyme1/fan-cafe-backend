package com.example.fan_cafe.order.saga.messaging;

import com.example.fan_cafe.order.payment.client.PaymentResultStatus;
import com.example.fan_cafe.order.saga.application.SagaCompensationService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PaymentRefundedResultConsumerTest {

    @Test
    void receivedRefundResultIsDelegatedToSagaCompensationService() {
        SagaCompensationService compensationService = mock(SagaCompensationService.class);
        PaymentRefundedResultConsumer consumer = new PaymentRefundedResultConsumer(compensationService);
        UUID sagaId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        PaymentRefundedResult result = new PaymentRefundedResult(
                PaymentRefundedResult.EVENT_TYPE,
                sagaId,
                10L,
                PaymentResultStatus.REFUNDED,
                "REFUND:" + sagaId,
                "order completion failed"
        );

        consumer.consume(result);

        verify(compensationService).complete(result);
    }
}
