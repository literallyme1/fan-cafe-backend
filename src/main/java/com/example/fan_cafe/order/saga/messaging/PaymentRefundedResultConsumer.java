package com.example.fan_cafe.order.saga.messaging;

import com.example.fan_cafe.order.saga.application.SagaCompensationService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.example.fan_cafe.outbox.mq.OutboxMQNames.PAYMENT_REFUND_RESULT_QUEUE;

@Component
@RequiredArgsConstructor
public class PaymentRefundedResultConsumer {
    private final SagaCompensationService compensationService;

    @RabbitListener(queues = PAYMENT_REFUND_RESULT_QUEUE)
    public void consume(PaymentRefundedResult result) {
        compensationService.complete(result);
    }
}
