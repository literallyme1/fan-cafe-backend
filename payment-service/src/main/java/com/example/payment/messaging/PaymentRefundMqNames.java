package com.example.payment.messaging;

public final class PaymentRefundMqNames {
    public static final String EXCHANGE = "outbox.exchange";
    public static final String COMMAND_ROUTING_KEY = "payment.refund.command";
    public static final String COMMAND_QUEUE = "payment.refund.command.payment.queue";
    public static final String RESULT_ROUTING_KEY = "payment.refund.result";

    private PaymentRefundMqNames() {
    }
}
