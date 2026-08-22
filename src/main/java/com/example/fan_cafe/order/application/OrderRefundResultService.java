package com.example.fan_cafe.order.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.exception.GlobalErrorCode;
import com.example.fan_cafe.merchandise.domain.Merchandise;
import com.example.fan_cafe.merchandise.exception.MerchandiseErrorCode;
import com.example.fan_cafe.merchandise.infrastructure.MerchandiseRepository;
import com.example.fan_cafe.order.domain.Order;
import com.example.fan_cafe.order.domain.OrderItem;
import com.example.fan_cafe.order.domain.OrderStatusHistory;
import com.example.fan_cafe.order.domain.Status;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
import com.example.fan_cafe.order.infrastructure.OrderStatusHistoryRepository;
import com.example.fan_cafe.order.payment.client.PaymentResultStatus;
import com.example.fan_cafe.order.payment.client.PaymentStatusResponse;
import com.example.fan_cafe.outbox.domain.OutboxEvent;
import com.example.fan_cafe.outbox.infrastructure.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderRefundResultService {
    private final OrderRepository orderRepository;
    private final MerchandiseRepository merchandiseRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Order apply(Long orderId, PaymentStatusResponse payment, String cancelReason) {
        if (!orderId.equals(payment.orderId()) || payment.status() != PaymentResultStatus.REFUNDED) {
            throw new CustomException(OrderErrorCode.PAYMENT_SERVICE_ERROR);
        }

        Order order = orderRepository.findPaymentOrderWithPessimisticLock(orderId)
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));
        Hibernate.initialize(order.getOrderItems());

        if (order.getStatus() == Status.REFUNDED) {
            log.info("[REFUND] - 이미 반영된 환불 결과 무시 (orderId={})", orderId);
            return order;
        }
        if (order.getStatus() != Status.PAID) {
            throw new CustomException(OrderErrorCode.ORDER_NOT_REFUNDABLE);
        }

        restoreStock(order);
        String reason = cancelReason == null || cancelReason.isBlank()
                ? "payment refund"
                : cancelReason.trim();
        Status from = order.getStatus();
        order.markRefunded();
        orderStatusHistoryRepository.save(OrderStatusHistory.of(order, from, Status.REFUNDED, reason));

        OutboxEvent outbox = persistOutboxWithEventId(OutboxEvent.init(
                "ORDER", orderId, buildPaymentRefundedPayload(order, reason, payment.refundIdempotencyKey())));
        log.info("[OUTBOX] - 환불 결과 이벤트 저장 (orderId={}, eventStatus={})", orderId, outbox.getStatus());
        return order;
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            Merchandise merchandise = merchandiseRepository.findMerchandiseWithPessimisticLock(item.getProductId())
                    .orElseThrow(() -> new CustomException(MerchandiseErrorCode.MERCHANDISE_NOT_FOUND));
            merchandise.increaseStock(item.getQuantity());
        }
    }

    private OutboxEvent persistOutboxWithEventId(OutboxEvent event) {
        OutboxEvent saved = outboxEventRepository.save(event);
        outboxEventRepository.flush();
        saved.assignEventIdFromPrimaryKey();
        return outboxEventRepository.save(saved);
    }

    private String buildPaymentRefundedPayload(Order order, String reason, String idempotencyKey) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "PAYMENT_REFUNDED");
        payload.put("orderId", order.getId());
        payload.put("userId", order.getUser().getId());
        payload.put("status", order.getStatus().name());
        payload.put("totalPrice", order.getTotalPrice());
        payload.put("cancelReason", reason);
        payload.put("idempotencyKey", idempotencyKey);
        payload.put("items", order.getOrderItems().stream().map(item -> Map.of(
                "productId", item.getProductId(),
                "quantity", item.getQuantity()
        )).toList());

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
