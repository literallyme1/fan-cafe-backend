package com.example.payment.application;

import com.example.payment.exception.PaymentErrorCode;
import com.example.payment.exception.PaymentException;
import com.example.payment.interfaces.dto.MockPgWebhookPayload;
import com.example.payment.interfaces.dto.PaymentResultResponse;
import com.example.payment.payment.MockPgWebhookSignatureVerifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class MockPgWebhookService {
    private static final String PAYMENT_APPROVED = "PAYMENT_APPROVED";
    private static final String PAYMENT_FAILED = "PAYMENT_FAILED";

    private final MockPgWebhookSignatureVerifier signatureVerifier;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public MockPgWebhookService(
            MockPgWebhookSignatureVerifier signatureVerifier,
            PaymentService paymentService,
            ObjectMapper objectMapper
    ) {
        this.signatureVerifier = signatureVerifier;
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    public PaymentResultResponse receive(
            String rawBody,
            String timestamp,
            String signature,
            BigDecimal expectedAmount
    ) {
        signatureVerifier.verify(timestamp, rawBody, signature);
        MockPgWebhookPayload payload = parse(rawBody);
        if (payload.getOrderId() == null) {
            throw new PaymentException(PaymentErrorCode.WEBHOOK_ORDER_ID_REQUIRED);
        }
        if (PAYMENT_APPROVED.equals(payload.getEventType())) {
            if (payload.getApprovalAmount() == null) {
                throw new PaymentException(PaymentErrorCode.WEBHOOK_APPROVAL_AMOUNT_REQUIRED);
            }
            return paymentService.approve(payload.getOrderId(), expectedAmount,
                    payload.getApprovalAmount(), payload.resolvePaymentKey());
        }
        if (PAYMENT_FAILED.equals(payload.getEventType())) {
            return paymentService.fail(payload.getOrderId(), expectedAmount, payload.getReason());
        }
        throw new PaymentException(PaymentErrorCode.INVALID_WEBHOOK_EVENT_TYPE);
    }

    private MockPgWebhookPayload parse(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, MockPgWebhookPayload.class);
        } catch (JsonProcessingException exception) {
            throw new PaymentException(PaymentErrorCode.INVALID_WEBHOOK_BODY);
        }
    }
}
