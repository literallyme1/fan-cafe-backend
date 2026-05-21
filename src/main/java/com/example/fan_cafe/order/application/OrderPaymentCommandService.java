package com.example.fan_cafe.order.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.exception.GlobalErrorCode;
import com.example.fan_cafe.order.domain.Order;
import com.example.fan_cafe.order.domain.OrderStatusHistory;
import com.example.fan_cafe.order.domain.Status;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
import com.example.fan_cafe.order.infrastructure.OrderStatusHistoryRepository;
import com.example.fan_cafe.outbox.domain.OutboxEvent;
import com.example.fan_cafe.outbox.infrastructure.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Mock 결제 승인/실패 공통 처리.
 * REST Mock API·Mock PG 웹훅이 동일 로직을 사용한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPaymentCommandService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * PAYMENT_PENDING → PAID (금액 일치 시). 성공 시 Outbox ORDER_CREATED 저장.
     *
     * @return 갱신된 주문 (이미 동일 키로 PAID면 상태 변경·Outbox 없음)
     */
    @Transactional
    public Order approvePayment(
            Order order,
            BigDecimal approvalAmount,
            String paymentKey,
            String historyReason
    ) {
        Long orderId = order.getId();

        if (order.isPaidWithPaymentKey(paymentKey)) {
            log.info("[MOCK-PAYMENT] - 중복 승인 요청 무시 (orderId={}, key={})", orderId, paymentKey);
            return order;
        }
        if (order.getStatus() == Status.PAID) {
            throw new CustomException(OrderErrorCode.ORDER_ALREADY_PAID);
        }
        if (order.getStatus() != Status.PAYMENT_PENDING) {
            throw new CustomException(OrderErrorCode.INVALID_PAYMENT_STATE);
        }

        if (order.getTotalPrice().compareTo(approvalAmount) != 0) {
            Status from = order.getStatus();
            order.markPaymentFailed();
            recordStatusHistory(order, from, Status.PAYMENT_FAILED, "approval amount mismatch");
            orderRepository.save(order);
            log.warn("[MOCK-PAYMENT] - 금액 불일치 (orderId={}, expected={}, actual={})",
                    orderId, order.getTotalPrice(), approvalAmount);
            throw new CustomException(OrderErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        Status from = order.getStatus();
        order.markPaid(paymentKey);
        recordStatusHistory(order, from, Status.PAID, historyReason);

        OutboxEvent orderCreatedOutbox = persistOutboxWithEventId(
                OutboxEvent.init("ORDER", order.getId(), buildOrderCreatedPayload(order)));
        log.info("[OUTBOX] - 결제 승인 후 이벤트 저장 (orderId={}, eventStatus={})",
                order.getId(), orderCreatedOutbox.getStatus());

        return order;
    }

    /** PAYMENT_PENDING → PAYMENT_FAILED. Outbox 저장 없음. */
    @Transactional
    public Order failPayment(Order order, String reason) {
        if (order.getStatus() == Status.PAYMENT_FAILED) {
            return order;
        }
        if (order.getStatus() != Status.PAYMENT_PENDING) {
            throw new CustomException(OrderErrorCode.INVALID_PAYMENT_STATE);
        }

        String resolvedReason = reason != null && !reason.isBlank()
                ? reason.trim()
                : "mock payment failed";

        Status from = order.getStatus();
        order.markPaymentFailed();
        recordStatusHistory(order, from, Status.PAYMENT_FAILED, resolvedReason);
        orderRepository.save(order);
        log.info("[MOCK-PAYMENT] - 결제 실패 처리 (orderId={})", order.getId());

        return order;
    }

    private void recordStatusHistory(Order order, Status from, Status to, String reason) {
        orderStatusHistoryRepository.save(OrderStatusHistory.of(order, from, to, reason));
    }

    private OutboxEvent persistOutboxWithEventId(OutboxEvent event) {
        OutboxEvent saved = outboxEventRepository.save(event);
        outboxEventRepository.flush();
        saved.assignEventIdFromPrimaryKey();
        return outboxEventRepository.save(saved);
    }

    private String buildOrderCreatedPayload(Order order) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "ORDER_CREATED");
        payload.put("orderId", order.getId());
        payload.put("userId", order.getUser().getId());
        payload.put("totalPrice", order.getTotalPrice());
        payload.put("items", order.getOrderItems().stream().map(i -> Map.of(
                "productId", i.getProductId(),
                "productName", i.getProductName(),
                "price", i.getPrice(),
                "quantity", i.getQuantity()
        )).toList());

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
