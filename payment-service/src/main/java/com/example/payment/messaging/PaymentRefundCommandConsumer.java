package com.example.payment.messaging;

import com.example.payment.application.PaymentService;
import com.example.payment.interfaces.dto.PaymentStatusResponse;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.example.payment.messaging.PaymentRefundMqNames.COMMAND_QUEUE;

@Component
public class PaymentRefundCommandConsumer {
    private final PaymentService paymentService;
    private final PaymentRefundResultPublisher resultPublisher;

    public PaymentRefundCommandConsumer(
            PaymentService paymentService,
            PaymentRefundResultPublisher resultPublisher
    ) {
        this.paymentService = paymentService;
        this.resultPublisher = resultPublisher;
    }

    @RabbitListener(queues = COMMAND_QUEUE)
    public void consume(RefundPaymentCommand command) {
        validate(command);
        PaymentStatusResponse payment = paymentService.refund(
                command.orderId(), command.sagaId(), command.reason());
        resultPublisher.publish(new PaymentRefundedResult(
                PaymentRefundedResult.EVENT_TYPE,
                command.sagaId(),
                payment.orderId(),
                payment.status(),
                payment.refundIdempotencyKey(),
                payment.refundReason()
        ));
    }

    private void validate(RefundPaymentCommand command) {
        String expectedKey = "REFUND:" + command.sagaId();
        if (!RefundPaymentCommand.EVENT_TYPE.equals(command.eventType())
                || command.sagaId() == null
                || command.orderId() == null
                || !expectedKey.equals(command.idempotencyKey())) {
            throw new IllegalArgumentException("Invalid REFUND_PAYMENT command");
        }
    }
}
