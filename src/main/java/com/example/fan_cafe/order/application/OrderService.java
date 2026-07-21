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
import com.example.fan_cafe.order.interfaces.dto.MockPaymentApproveRequest;
import com.example.fan_cafe.order.interfaces.dto.MockPaymentCancelRequest;
import com.example.fan_cafe.order.interfaces.dto.MockPaymentFailRequest;
import com.example.fan_cafe.order.interfaces.dto.OrderCreateRequest;
import com.example.fan_cafe.order.interfaces.dto.OrderCreateResponse;
import com.example.fan_cafe.order.interfaces.dto.OrderQueryResponse;
import com.example.fan_cafe.outbox.domain.OutboxEvent;
import com.example.fan_cafe.outbox.infrastructure.OutboxEventRepository;
import com.example.fan_cafe.user.domain.User;
import com.example.fan_cafe.user.exception.UserErrorCode;
import com.example.fan_cafe.user.infrastructure.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserRepository userRepository;
    private final MerchandiseRepository merchandiseRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderPaymentCommandService orderPaymentCommandService;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public OrderQueryResponse get(User user, Long orderId) {
        getOrderer(user);
        Order order = orderRepository.findByIdAndUserIdWithItems(orderId, user.getId())
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));
        return OrderQueryResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderQueryResponse> getMyOrders(User user) {
        getOrderer(user);
        return orderRepository.findAllByUserIdWithItems(user.getId()).stream()
                .map(OrderQueryResponse::from)
                .toList();
    }

    @Transactional
    public OrderCreateResponse create(User user, OrderCreateRequest request) {
        User orderer = getOrderer(user);

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new CustomException(OrderErrorCode.ORDER_ITEMS_REQUIRED);
        }

        Order order = Order.paymentPending(orderer, BigDecimal.ZERO);
        BigDecimal total = BigDecimal.ZERO;

        for (OrderCreateRequest.Item item : request.getItems()) {
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            if (quantity < 1) {
                throw new CustomException(OrderErrorCode.INVALID_QUANTITY);
            }

            Merchandise merchandise = merchandiseRepository.findMerchandiseWithPessimisticLock(item.getProductId())
                    .orElseThrow(() -> new CustomException(MerchandiseErrorCode.MERCHANDISE_NOT_FOUND));

            BigDecimal unitPrice = resolveUnitPrice(merchandise);
            merchandise.decreaseStock(quantity);

            order.addItem(OrderItem.snapshot(
                    merchandise.getId(),
                    merchandise.getName(),
                    unitPrice,
                    quantity
            ));

            total = total.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        }

        order.updateTotalPrice(total);
        Order saved = orderRepository.save(order);
        log.info("[ORDER] - 주문 생성 완료 (OrderNo: {}, status: {})", saved.getId(), saved.getStatus());

        return OrderCreateResponse.from(saved);
    }

    @Transactional
    public OrderQueryResponse approveMockPayment(User user, Long orderId, MockPaymentApproveRequest request) {
        getOrderer(user);
        String paymentKey = request.resolvePaymentKey();
        if (paymentKey == null) {
            throw new CustomException(OrderErrorCode.PAYMENT_KEY_REQUIRED);
        }

        Order order = orderRepository.findByIdAndUserIdWithItems(orderId, user.getId())
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));

        Order updated = orderPaymentCommandService.approvePaymentWithPessimisticLock(
                order,
                request.getApprovalAmount(),
                paymentKey,
                "mock payment approved"
        );
        return OrderQueryResponse.from(updated);
    }

    @Transactional
    public OrderQueryResponse failMockPayment(User user, Long orderId, MockPaymentFailRequest request) {
        getOrderer(user);

        Order order = orderRepository.findByIdAndUserIdWithItems(orderId, user.getId())
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));

        String reason = request.getReason() != null && !request.getReason().isBlank()
                ? request.getReason().trim()
                : "mock payment failed";

        Order updated = orderPaymentCommandService.failPayment(order, reason);
        return OrderQueryResponse.from(updated);
    }

    @Transactional
    public OrderQueryResponse cancelMockPayment(User user, Long orderId, MockPaymentCancelRequest request) {
        getOrderer(user);

        Order order = orderRepository.findByIdAndUserIdWithItems(orderId, user.getId())
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));

        restoreStock(order);

        Order updated = orderPaymentCommandService.cancelPayment(
                order,
                request.getCancelReason(),
                request.getIdempotencyKey()
        );
        return OrderQueryResponse.from(updated);
    }

    @Transactional
    public OrderQueryResponse cancel(User user, Long orderId) {
        getOrderer(user);

        Order order = orderRepository.findByIdAndUserIdWithItems(orderId, user.getId())
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));

        if (!order.cancellable()) {
            throw new CustomException(OrderErrorCode.ORDER_NOT_CANCELLABLE);
        }

        restoreStock(order);

        Status from = order.getStatus();
        order.cancel();
        recordStatusHistory(order, from, Status.CANCELLED, "order cancelled");

        persistOutboxWithEventId(OutboxEvent.init("ORDER", order.getId(), buildOrderCancelledPayload(order)));

        return OrderQueryResponse.from(order);
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

    private void restoreStock(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            Merchandise merchandise = merchandiseRepository.findMerchandiseWithPessimisticLock(item.getProductId())
                    .orElseThrow(() -> new CustomException(MerchandiseErrorCode.MERCHANDISE_NOT_FOUND));
            merchandise.increaseStock(item.getQuantity());
        }
    }

    private User getOrderer(User user) {
        return userRepository.findByIdAndDeletedAtIsNull(user.getId())
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }

    private BigDecimal resolveUnitPrice(Merchandise merchandise) {
        Long salePrice = merchandise.getSalePrice();
        Long price = salePrice != null ? salePrice : merchandise.getPrice();
        return BigDecimal.valueOf(price);
    }

    private String buildOrderCancelledPayload(Order order) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "ORDER_CANCELLED");
        payload.put("orderId", order.getId());
        payload.put("userId", order.getUser().getId());
        payload.put("status", order.getStatus().name());
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
}
