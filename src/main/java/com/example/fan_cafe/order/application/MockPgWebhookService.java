package com.example.fan_cafe.order.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.order.domain.Order;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
import com.example.fan_cafe.order.interfaces.dto.MockPgWebhookPayload;
import com.example.fan_cafe.order.interfaces.dto.OrderQueryResponse;
import com.example.fan_cafe.order.payment.client.PaymentClient;
import com.example.fan_cafe.order.payment.client.PaymentResultResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MockPgWebhookService {

    private final OrderRepository orderRepository;
    private final OrderPaymentResultService orderPaymentResultService;
    private final PaymentClient paymentClient;
    private final ObjectMapper objectMapper;

    public OrderQueryResponse receive(
            String rawBody,
            String timestamp,
            String signature
    ) {
        MockPgWebhookPayload payload = parsePayload(rawBody);

        Order order = orderRepository.findByIdWithItems(payload.getOrderId())
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));

        PaymentResultResponse result = paymentClient.forwardWebhook(
                rawBody, timestamp, signature, order.getTotalPrice());
        OrderQueryResponse updated = orderPaymentResultService.apply(
                order.getId(), result, "mock pg webhook approved", "mock pg webhook payment failed");
        log.info("[MOCK-PG-WEBHOOK] - Payment 결과 반영 (orderId={}, status={})",
                updated.getOrderId(), updated.getStatus());
        return updated;
    }

    private MockPgWebhookPayload parsePayload(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, MockPgWebhookPayload.class);
        } catch (JsonProcessingException e) {
            throw new CustomException(OrderErrorCode.INVALID_WEBHOOK_EVENT_TYPE);
        }
    }
}
