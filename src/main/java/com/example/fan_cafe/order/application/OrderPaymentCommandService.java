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

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPaymentCommandService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Order applyPaymentApproved(Long orderId, String historyReason) {
        Order lockedOrder = orderRepository.findPaymentOrderWithPessimisticLock(orderId)
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));

        if (lockedOrder.getStatus() == Status.PAID) {
            log.info("[PAYMENT] - 이미 반영된 승인 결과 무시 (orderId={})", orderId);
            return lockedOrder;
        }
        if (lockedOrder.getStatus() != Status.PAYMENT_PENDING) {
            throw new CustomException(OrderErrorCode.INVALID_PAYMENT_STATE);
        }

        Status from = lockedOrder.getStatus();
        lockedOrder.markPaid();
        recordStatusHistory(lockedOrder, from, Status.PAID, historyReason);

        OutboxEvent orderPaidOutbox = persistOutboxWithEventId(
                OutboxEvent.init("ORDER", lockedOrder.getId(), buildOrderPaidPayload(lockedOrder)));
        log.info("[OUTBOX] - 결제 승인 후 이벤트 저장 (orderId={}, eventStatus={})",
                lockedOrder.getId(), orderPaidOutbox.getStatus());

        return lockedOrder;
    }

    @Transactional
    public Order applyPaymentFailed(Long orderId, String reason) {
        Order lockedOrder = orderRepository.findPaymentOrderWithPessimisticLock(orderId)
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));

        if (lockedOrder.getStatus() == Status.PAYMENT_FAILED) {
            return lockedOrder;
        }
        if (lockedOrder.getStatus() != Status.PAYMENT_PENDING) {
            throw new CustomException(OrderErrorCode.INVALID_PAYMENT_STATE);
        }

        String resolvedReason = reason != null && !reason.isBlank()
                ? reason.trim()
                : "mock payment failed";

        Status from = lockedOrder.getStatus();
        lockedOrder.markPaymentFailed();
        recordStatusHistory(lockedOrder, from, Status.PAYMENT_FAILED, resolvedReason);
        log.info("[PAYMENT] - 결제 실패 결과 반영 (orderId={})", lockedOrder.getId());

        return lockedOrder;
    }

    @Transactional
    public Order cancelPayment(Order order, String cancelReason, String idempotencyKey) {
        Long orderId = order.getId();

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new CustomException(OrderErrorCode.CANCEL_IDEMPOTENCY_KEY_REQUIRED);
        }
        String key = idempotencyKey.trim();

        if (order.isRefundedWithIdempotencyKey(key)) {
            log.info("[MOCK-PAYMENT] - 중복 취소/환불 요청 무시 (orderId={}, key={})", orderId, key);
            return order;
        }
        if (order.isTerminalRefundOrCancel()) {
            throw new CustomException(OrderErrorCode.ORDER_ALREADY_REFUNDED);
        }
        if (order.getStatus() != Status.PAID) {
            throw new CustomException(OrderErrorCode.ORDER_NOT_REFUNDABLE);
        }

        String resolvedReason = cancelReason != null && !cancelReason.isBlank()
                ? cancelReason.trim()
                : "mock payment refund";

        Status from = order.getStatus();
        order.markRefunded(key);
        recordStatusHistory(order, from, Status.REFUNDED, resolvedReason);

        OutboxEvent refundOutbox = persistOutboxWithEventId(
                OutboxEvent.init("ORDER", order.getId(), buildPaymentRefundedPayload(order, resolvedReason, key)));
        log.info("[OUTBOX] - Mock 취소/환불 후 이벤트 저장 (orderId={}, eventStatus={})",
                order.getId(), refundOutbox.getStatus());

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

    private String buildPaymentRefundedPayload(Order order, String cancelReason, String idempotencyKey) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "PAYMENT_REFUNDED");
        payload.put("orderId", order.getId());
        payload.put("userId", order.getUser().getId());
        payload.put("status", order.getStatus().name());
        payload.put("totalPrice", order.getTotalPrice());
        payload.put("cancelReason", cancelReason);
        payload.put("idempotencyKey", idempotencyKey);
        payload.put("items", order.getOrderItems().stream().map(i -> Map.of(
                "productId", i.getProductId(),
                "quantity", i.getQuantity()
        )).toList());

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String buildOrderPaidPayload(Order order) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "ORDER_PAID");
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
