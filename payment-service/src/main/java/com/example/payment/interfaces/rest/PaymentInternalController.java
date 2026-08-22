package com.example.payment.interfaces.rest;

import com.example.payment.application.MockPgWebhookService;
import com.example.payment.application.PaymentService;
import com.example.payment.interfaces.dto.PaymentApproveRequest;
import com.example.payment.interfaces.dto.PaymentFailRequest;
import com.example.payment.interfaces.dto.PaymentResultResponse;
import com.example.payment.interfaces.dto.PaymentRefundRequest;
import com.example.payment.interfaces.dto.PaymentStatusResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/internal")
public class PaymentInternalController {
    private final PaymentService paymentService;
    private final MockPgWebhookService webhookService;

    public PaymentInternalController(PaymentService paymentService, MockPgWebhookService webhookService) {
        this.paymentService = paymentService;
        this.webhookService = webhookService;
    }

    @PostMapping("/payments/{orderId}/approve")
    public PaymentResultResponse approve(
            @PathVariable Long orderId,
            @RequestBody @Valid PaymentApproveRequest request
    ) {
        return paymentService.approve(orderId, request.expectedAmount(),
                request.approvalAmount(), request.paymentKey());
    }

    @PostMapping("/payments/{orderId}/fail")
    public PaymentResultResponse fail(
            @PathVariable Long orderId,
            @RequestBody @Valid PaymentFailRequest request
    ) {
        return paymentService.fail(orderId, request.expectedAmount(), request.reason());
    }

    @GetMapping("/payments/{orderId}")
    public PaymentStatusResponse getStatus(@PathVariable Long orderId) {
        return paymentService.getStatus(orderId);
    }

    @PostMapping("/payments/{orderId}/refund")
    public PaymentStatusResponse refund(
            @PathVariable Long orderId,
            @RequestBody @Valid PaymentRefundRequest request
    ) {
        return paymentService.refund(orderId, request.sagaId(), request.reason());
    }

    /** 이번 Step의 임시 호환 엔드포인트: 외부 웹훅 URL은 아직 Order가 유지한다. */
    @PostMapping("/mock-pg/webhook")
    public PaymentResultResponse webhook(
            @RequestBody String rawBody,
            @RequestHeader("X-Mock-PG-Timestamp") String timestamp,
            @RequestHeader("X-Mock-PG-Signature") String signature,
            @RequestParam BigDecimal expectedAmount
    ) {
        return webhookService.receive(rawBody, timestamp, signature, expectedAmount);
    }
}
