package com.example.payment.exception;

import org.springframework.http.HttpStatus;

public enum PaymentErrorCode {
    PAYMENT_KEY_REQUIRED("P001", HttpStatus.BAD_REQUEST, "paymentKey is required"),
    PAYMENT_AMOUNT_REQUIRED("P002", HttpStatus.BAD_REQUEST, "payment amount is required"),
    EXPECTED_AMOUNT_CONFLICT("P004", HttpStatus.CONFLICT, "order amount conflicts with the stored payment amount"),
    PAYMENT_ALREADY_APPROVED("P005", HttpStatus.CONFLICT, "payment is already approved"),
    INVALID_PAYMENT_STATE("P006", HttpStatus.CONFLICT, "payment cannot be changed from its current state"),
    WEBHOOK_SIGNATURE_INVALID("P010", HttpStatus.UNAUTHORIZED, "webhook signature is invalid"),
    WEBHOOK_TIMESTAMP_INVALID("P011", HttpStatus.BAD_REQUEST, "webhook timestamp is invalid"),
    WEBHOOK_TIMESTAMP_EXPIRED("P012", HttpStatus.BAD_REQUEST, "webhook timestamp has expired"),
    INVALID_WEBHOOK_EVENT_TYPE("P013", HttpStatus.BAD_REQUEST, "webhook event type is not supported"),
    WEBHOOK_APPROVAL_AMOUNT_REQUIRED("P014", HttpStatus.BAD_REQUEST, "approvalAmount is required for approval webhook"),
    WEBHOOK_ORDER_ID_REQUIRED("P015", HttpStatus.BAD_REQUEST, "orderId is required for webhook"),
    INVALID_WEBHOOK_BODY("P016", HttpStatus.BAD_REQUEST, "webhook body is invalid");

    private final String code;
    private final HttpStatus status;
    private final String message;

    PaymentErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    public String getCode() { return code; }
    public HttpStatus getStatus() { return status; }
    public String getMessage() { return message; }
}
