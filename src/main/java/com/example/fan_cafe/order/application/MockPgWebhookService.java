package com.example.fan_cafe.order.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.exception.GlobalErrorCode;
import com.example.fan_cafe.order.domain.Order;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
import com.example.fan_cafe.order.interfaces.dto.MockPgWebhookPayload;
import com.example.fan_cafe.order.interfaces.dto.OrderQueryResponse;
import com.example.fan_cafe.order.payment.MockPgWebhookSignatureVerifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MockPgWebhookService {

    private static final String EVENT_PAYMENT_APPROVED = "PAYMENT_APPROVED";
    private static final String EVENT_PAYMENT_FAILED = "PAYMENT_FAILED";

    private final MockPgWebhookSignatureVerifier signatureVerifier;
    private final OrderRepository orderRepository;
    private final OrderPaymentCommandService orderPaymentCommandService;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderQueryResponse receive(
            String rawBody,
            String timestamp,
            String signature
    ) {
        signatureVerifier.verify(timestamp, rawBody, signature);
        MockPgWebhookPayload payload = parsePayload(rawBody);

        Order order = orderRepository.findByIdWithItems(payload.getOrderId())
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));

        String eventType = payload.getEventType();
        if (EVENT_PAYMENT_APPROVED.equals(eventType)) {
            return handleApproved(order, payload);
        }
        if (EVENT_PAYMENT_FAILED.equals(eventType)) {
            return handleFailed(order, payload);
        }
        throw new CustomException(OrderErrorCode.INVALID_WEBHOOK_EVENT_TYPE);
    }

    private OrderQueryResponse handleApproved(Order order, MockPgWebhookPayload payload) {
        String paymentKey = payload.resolvePaymentKey();
        if (paymentKey == null) {
            throw new CustomException(OrderErrorCode.PAYMENT_KEY_REQUIRED);
        }
        if (payload.getApprovalAmount() == null) {
            throw new CustomException(OrderErrorCode.WEBHOOK_APPROVAL_AMOUNT_REQUIRED);
        }

        Order updated = orderPaymentCommandService.approvePaymentWithPessimisticLock(
                order,
                payload.getApprovalAmount(),
                paymentKey,
                "mock pg webhook approved"
        );
        log.info("[MOCK-PG-WEBHOOK] - PAYMENT_APPROVED 처리 (orderId={})", updated.getId());
        return OrderQueryResponse.from(updated);
    }

    private OrderQueryResponse handleFailed(Order order, MockPgWebhookPayload payload) {
        String reason = payload.getReason() != null && !payload.getReason().isBlank()
                ? payload.getReason().trim()
                : "mock pg webhook payment failed";

        Order updated = orderPaymentCommandService.failPayment(order, reason);
        log.info("[MOCK-PG-WEBHOOK] - PAYMENT_FAILED 처리 (orderId={})", updated.getId());
        return OrderQueryResponse.from(updated);
    }

    private MockPgWebhookPayload parsePayload(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, MockPgWebhookPayload.class);
        } catch (JsonProcessingException e) {
            throw new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
