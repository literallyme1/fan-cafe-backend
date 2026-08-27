package com.example.payment.messaging;

import com.example.payment.application.PaymentService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentApprovalCommandConsumer {
    private final PaymentService paymentService;

    public PaymentApprovalCommandConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @RabbitListener(queues = PaymentRefundMqNames.APPROVAL_COMMAND_QUEUE)
    public void consume(ApprovePaymentCommand command) {
        validate(command);
        paymentService.approve(
                command.orderId(),
                command.expectedAmount(),
                command.approvalAmount(),
                command.paymentKey());
    }

    private void validate(ApprovePaymentCommand command) {
        if (command == null
                || !ApprovePaymentCommand.EVENT_TYPE.equals(command.eventType())
                || command.orderId() == null
                || command.expectedAmount() == null
                || command.approvalAmount() == null
                || command.paymentKey() == null
                || command.paymentKey().isBlank()) {
            throw new IllegalArgumentException("Invalid APPROVE_PAYMENT command");
        }
    }
}
