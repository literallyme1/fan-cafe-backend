package com.example.fan_cafe.order.saga.application;

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
import com.example.fan_cafe.order.saga.domain.PaymentSagaStateMachine;
import com.example.fan_cafe.order.saga.domain.SagaInstance;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import com.example.fan_cafe.order.saga.exception.SagaErrorCode;
import com.example.fan_cafe.order.saga.infrastructure.SagaInstanceRepository;
import com.example.fan_cafe.order.saga.messaging.PaymentRefundedResult;
import com.example.fan_cafe.order.saga.messaging.RefundPaymentCommand;
import com.example.fan_cafe.outbox.domain.OutboxEvent;
import com.example.fan_cafe.outbox.infrastructure.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SagaCompensationService {
    private static final String AGGREGATE_TYPE = "PAYMENT_SAGA";

    private final OrderRepository orderRepository;
    private final MerchandiseRepository merchandiseRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final SagaInstanceRepository sagaRepository;
    private final PaymentSagaStateMachine stateMachine;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void start(UUID sagaId, Long orderId, String reason) {
        SagaInstance saga = findSagaForUpdate(sagaId);
        validateOrder(saga, orderId);

        if (saga.getStatus() == SagaStatus.COMPENSATING
                || saga.getStatus() == SagaStatus.COMPENSATED
                || saga.getStatus() == SagaStatus.COMPLETED) {
            return;
        }

        RefundPaymentCommand command = RefundPaymentCommand.of(sagaId, orderId, reason);
        stateMachine.transition(saga, SagaStatus.COMPENSATING);
        persistOutbox(OutboxEvent.init(
                AGGREGATE_TYPE, orderId, serialize(command)));
    }

    @Transactional
    public void complete(PaymentRefundedResult result) {
        validateResult(result);

        Order order = orderRepository.findPaymentOrderWithPessimisticLock(result.orderId())
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));
        SagaInstance saga = findSagaForUpdate(result.sagaId());
        validateOrder(saga, result.orderId());

        if (saga.getStatus() == SagaStatus.COMPENSATED && order.getStatus() == Status.REFUNDED) {
            return;
        }
        if (order.getStatus() != Status.PAYMENT_PENDING) {
            throw new CustomException(OrderErrorCode.INVALID_PAYMENT_STATE);
        }

        restoreStock(order);
        Status from = order.getStatus();
        order.markCompensatedRefunded();
        orderStatusHistoryRepository.save(OrderStatusHistory.of(
                order, from, Status.REFUNDED, resolveReason(result.refundReason())));
        persistOutbox(OutboxEvent.init(
                "ORDER", order.getId(), buildPaymentRefundedPayload(order, result)));
        stateMachine.transition(saga, SagaStatus.COMPENSATED);
    }

    private SagaInstance findSagaForUpdate(UUID sagaId) {
        return sagaRepository.findBySagaIdForUpdate(sagaId)
                .orElseThrow(() -> new CustomException(SagaErrorCode.SAGA_NOT_FOUND));
    }

    private void validateOrder(SagaInstance saga, Long orderId) {
        if (!saga.getOrderId().equals(orderId)) {
            throw new CustomException(SagaErrorCode.SAGA_NOT_FOUND);
        }
    }

    private void validateResult(PaymentRefundedResult result) {
        String expectedKey = "REFUND:" + result.sagaId();
        if (!PaymentRefundedResult.EVENT_TYPE.equals(result.eventType())
                || result.status() != PaymentResultStatus.REFUNDED
                || !expectedKey.equals(result.refundIdempotencyKey())) {
            throw new CustomException(OrderErrorCode.PAYMENT_SERVICE_ERROR);
        }
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            Merchandise merchandise = merchandiseRepository.findMerchandiseWithPessimisticLock(item.getProductId())
                    .orElseThrow(() -> new CustomException(MerchandiseErrorCode.MERCHANDISE_NOT_FOUND));
            merchandise.increaseStock(item.getQuantity());
        }
    }

    private String buildPaymentRefundedPayload(Order order, PaymentRefundedResult result) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", PaymentRefundedResult.EVENT_TYPE);
        payload.put("sagaId", result.sagaId());
        payload.put("orderId", order.getId());
        payload.put("userId", order.getUser().getId());
        payload.put("status", order.getStatus().name());
        payload.put("refundIdempotencyKey", result.refundIdempotencyKey());
        return serialize(payload);
    }

    private String resolveReason(String reason) {
        return reason == null || reason.isBlank() ? "saga compensation refund" : reason.trim();
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException serializationFailure) {
            throw new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void persistOutbox(OutboxEvent event) {
        OutboxEvent saved = outboxEventRepository.save(event);
        outboxEventRepository.flush();
        saved.assignEventIdFromPrimaryKey();
        outboxEventRepository.save(saved);
    }
}
