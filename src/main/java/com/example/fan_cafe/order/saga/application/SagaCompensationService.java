package com.example.fan_cafe.order.saga.application;

import com.example.fan_cafe.campaign.application.CampaignRefundStateService;
import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.exception.GlobalErrorCode;
import com.example.fan_cafe.merchandise.domain.Merchandise;
import com.example.fan_cafe.merchandise.exception.MerchandiseErrorCode;
import com.example.fan_cafe.merchandise.infrastructure.MerchandiseRepository;
import com.example.fan_cafe.order.domain.Order;
import com.example.fan_cafe.order.domain.OrderItem;
import com.example.fan_cafe.order.domain.OrderType;
import com.example.fan_cafe.order.domain.OrderStatusHistory;
import com.example.fan_cafe.order.domain.Status;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
import com.example.fan_cafe.order.infrastructure.OrderStatusHistoryRepository;
import com.example.fan_cafe.order.interfaces.dto.OrderQueryResponse;
import com.example.fan_cafe.order.payment.client.PaymentResultStatus;
import com.example.fan_cafe.order.saga.domain.PaymentSagaStateMachine;
import com.example.fan_cafe.order.saga.domain.SagaInstance;
import com.example.fan_cafe.order.saga.domain.SagaStatus;
import com.example.fan_cafe.order.saga.exception.SagaErrorCode;
import com.example.fan_cafe.order.saga.infrastructure.SagaInstanceRepository;
import com.example.fan_cafe.order.saga.messaging.PaymentRefundedResult;
import com.example.fan_cafe.order.saga.messaging.RefundPaymentCommand;
import com.example.fan_cafe.order.saga.recovery.SagaRecoveryProperties;
import com.example.fan_cafe.outbox.domain.OutboxEvent;
import com.example.fan_cafe.outbox.infrastructure.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.time.Clock;
import java.time.LocalDateTime;

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
    private final SagaRecoveryProperties recoveryProperties;
    private final Clock clock;
    private final CampaignRefundStateService campaignRefundStateService;

    @Transactional
    public void start(UUID sagaId, Long orderId, String reason) {
        SagaInstance saga = findSagaForUpdate(sagaId);
        validateOrder(saga, orderId);

        if (saga.getStatus() == SagaStatus.COMPENSATING
                || saga.getStatus() == SagaStatus.COMPENSATED
                || saga.getStatus() == SagaStatus.COMPLETED) {
            return;
        }

        startCompensation(saga, orderId, reason);
    }

    @Transactional
    public void startAfterOrderCompletionFailure(
            UUID sagaId,
            Long orderId,
            String reason,
            LocalDateTime approvedAt
    ) {
        Order order = orderRepository.findPaymentOrderWithPessimisticLock(orderId)
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));
        if (order.getOrderType() == OrderType.CAMPAIGN_CONTRIBUTION) {
            startCampaign(sagaId, orderId, reason, approvedAt);
            return;
        }
        start(sagaId, orderId, reason);
    }

    @Transactional
    public void requestManual(UUID sagaId, Long orderId, String reason) {
        SagaInstance saga = findSagaForUpdate(sagaId);
        validateOrder(saga, orderId);

        if (saga.getStatus() == SagaStatus.COMPENSATED) {
            return;
        }
        if (saga.getStatus() == SagaStatus.COMPENSATING) {
            LocalDateTime now = LocalDateTime.now(clock);
            if (saga.getNextRetryAt() != null && saga.getNextRetryAt().isAfter(now)) {
                return;
            }
            persistRefundCommand(saga, orderId, reason);
            saga.scheduleInitialRefundResultDeadline(
                    now.plus(recoveryProperties.getRefundResultTimeout()));
            return;
        }
        if (saga.getStatus() != SagaStatus.PAYMENT_COMPLETED
                && saga.getStatus() != SagaStatus.RECONCILIATION_REQUIRED) {
            throw new CustomException(SagaErrorCode.INVALID_MANUAL_ACTION);
        }
        startCompensation(saga, orderId, reason);
    }

    @Transactional
    public OrderQueryResponse startCampaign(
            UUID sagaId,
            Long orderId,
            String reason,
            LocalDateTime approvedAt
    ) {
        Order order = orderRepository.findPaymentOrderWithPessimisticLock(orderId)
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));
        if (order.getOrderType() != OrderType.CAMPAIGN_CONTRIBUTION) {
            throw new CustomException(OrderErrorCode.INVALID_PAYMENT_STATE);
        }
        SagaInstance saga = findSagaForUpdate(sagaId);
        validateOrder(saga, orderId);
        if (saga.getStatus() == SagaStatus.COMPENSATED
                || saga.getStatus() == SagaStatus.COMPENSATING) {
            return OrderQueryResponse.from(order);
        }
        if (saga.getStatus() != SagaStatus.PAYMENT_COMPLETED
                && saga.getStatus() != SagaStatus.COMPLETED) {
            throw new CustomException(SagaErrorCode.INVALID_SAGA_TRANSITION);
        }

        campaignRefundStateService.begin(orderId, approvedAt);
        stateMachine.transitionCampaignCompensation(saga);
        saga.scheduleInitialRefundResultDeadline(
                LocalDateTime.now(clock).plus(recoveryProperties.getRefundResultTimeout()));
        persistRefundCommand(saga, orderId, reason);
        return OrderQueryResponse.from(order);
    }

    @Transactional
    public OrderQueryResponse startCampaignUserRefund(
            UUID sagaId,
            Long orderId,
            Long campaignId,
            Long userId
    ) {
        Order order = orderRepository.findPaymentOrderWithPessimisticLock(orderId)
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));
        if (order.getOrderType() != OrderType.CAMPAIGN_CONTRIBUTION) {
            throw new CustomException(OrderErrorCode.INVALID_PAYMENT_STATE);
        }
        SagaInstance saga = findSagaForUpdate(sagaId);
        validateOrder(saga, orderId);
        if (saga.getStatus() == SagaStatus.COMPENSATED
                || saga.getStatus() == SagaStatus.COMPENSATING) {
            return OrderQueryResponse.from(order);
        }
        campaignRefundStateService.validateUserRefund(orderId, campaignId, userId);
        return startCampaign(sagaId, orderId,
                com.example.fan_cafe.order.saga.domain.SagaCompensationReason.USER_REFUND.name(), null);
    }

    @Transactional
    public Optional<OrderQueryResponse> startLateSuccessIfOrderCannotComplete(
            UUID sagaId,
            Long orderId
    ) {
        Order order = orderRepository.findPaymentOrderWithPessimisticLock(orderId)
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));
        SagaInstance saga = findSagaForUpdate(sagaId);
        validateOrder(saga, orderId);

        if (order.getStatus() == Status.PAYMENT_PENDING || order.getStatus() == Status.PAID) {
            return Optional.empty();
        }
        if (order.getStatus() != Status.CANCELLED) {
            throw new CustomException(OrderErrorCode.INVALID_PAYMENT_STATE);
        }
        if (saga.getStatus() != SagaStatus.COMPENSATING
                && saga.getStatus() != SagaStatus.COMPENSATED) {
            startCompensation(saga, orderId, "late payment approval after order cancellation");
        }
        return Optional.of(OrderQueryResponse.from(order));
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
        boolean campaignPaid = order.getOrderType() == OrderType.CAMPAIGN_CONTRIBUTION
                && order.getStatus() == Status.PAID;
        if (order.getStatus() != Status.PAYMENT_PENDING
                && order.getStatus() != Status.CANCELLED
                && !campaignPaid) {
            throw new CustomException(OrderErrorCode.INVALID_PAYMENT_STATE);
        }

        if (order.getStatus() == Status.PAYMENT_PENDING) {
            restoreStock(order);
        }
        Status from = order.getStatus();
        campaignRefundStateService.complete(order.getId());
        order.markCompensatedRefunded();
        orderStatusHistoryRepository.save(OrderStatusHistory.of(
                order, from, Status.REFUNDED, resolveReason(result.refundReason())));
        persistOutbox(OutboxEvent.init(
                "ORDER", order.getId(), buildPaymentRefundedPayload(order, result)));
        stateMachine.transition(saga, SagaStatus.COMPENSATED);
    }

    private void startCompensation(SagaInstance saga, Long orderId, String reason) {
        stateMachine.transition(saga, SagaStatus.COMPENSATING);
        saga.scheduleInitialRefundResultDeadline(
                LocalDateTime.now(clock).plus(recoveryProperties.getRefundResultTimeout()));
        persistRefundCommand(saga, orderId, reason);
    }

    private void persistRefundCommand(SagaInstance saga, Long orderId, String reason) {
        RefundPaymentCommand command = RefundPaymentCommand.of(saga.getSagaId(), orderId, reason);
        persistOutbox(OutboxEvent.init(
                AGGREGATE_TYPE, orderId, serialize(command)));
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
