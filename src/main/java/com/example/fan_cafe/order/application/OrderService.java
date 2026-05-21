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
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    // 로그인 사용자 기준으로 본인 주문 단건을 조회한다.
    @Transactional(readOnly = true)
    public OrderQueryResponse get(User user, Long orderId) {
        getOrderer(user);
        Order order = orderRepository.findByIdAndUserIdWithItems(orderId, user.getId())
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));
        return OrderQueryResponse.from(order);
    }

    // 로그인 사용자의 주문 목록을 최신순 조회
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

        // Mock PG 승인 전까지 PAYMENT_PENDING — Outbox는 승인 시점에만 저장한다.
        Order order = Order.paymentPending(orderer, BigDecimal.ZERO);
        BigDecimal total = BigDecimal.ZERO;

        for (OrderCreateRequest.Item item : request.getItems()) {
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            if (quantity < 1) {
                throw new CustomException(OrderErrorCode.INVALID_QUANTITY);
            }

            Merchandise merchandise = merchandiseRepository.findByIdAndDeletedAtIsNullForUpdate(item.getProductId())
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

    /**
     * Mock PG 승인. PAYMENT_PENDING → PAID 성공 시에만 Outbox(ORDER_CREATED)를 같은 트랜잭션에 저장한다.
     */
    @Transactional
    public OrderQueryResponse approveMockPayment(User user, Long orderId, MockPaymentApproveRequest request) {
        getOrderer(user);
        String paymentKey = request.resolvePaymentKey();
        if (paymentKey == null) {
            throw new CustomException(OrderErrorCode.PAYMENT_KEY_REQUIRED);
        }

        Order order = orderRepository.findByIdAndUserIdWithItems(orderId, user.getId())
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));

        // 이미 PAID + 동일 키 → 멱등 응답 (Outbox·이력 추가 없음)
        if (order.isPaidWithPaymentKey(paymentKey)) {
            log.info("[MOCK-PAYMENT] - 중복 승인 요청 무시 (orderId={}, key={})", orderId, paymentKey);
            return OrderQueryResponse.from(order);
        }
        if (order.getStatus() == Status.PAID) {
            throw new CustomException(OrderErrorCode.ORDER_ALREADY_PAID);
        }
        if (order.getStatus() != Status.PAYMENT_PENDING) {
            throw new CustomException(OrderErrorCode.INVALID_PAYMENT_STATE);
        }

        if (order.getTotalPrice().compareTo(request.getApprovalAmount()) != 0) {
            Status from = order.getStatus();
            order.markPaymentFailed();
            recordStatusHistory(order, from, Status.PAYMENT_FAILED, "approval amount mismatch");
            orderRepository.save(order);
            log.warn("[MOCK-PAYMENT] - 금액 불일치 (orderId={}, expected={}, actual={})",
                    orderId, order.getTotalPrice(), request.getApprovalAmount());
            throw new CustomException(OrderErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        Status from = order.getStatus();
        order.markPaid(paymentKey);
        recordStatusHistory(order, from, Status.PAID, "mock payment approved");

        OutboxEvent orderCreatedOutbox = persistOutboxWithEventId(
                OutboxEvent.init("ORDER", order.getId(), buildOrderCreatedPayload(order)));
        log.info("[OUTBOX] - 결제 승인 후 이벤트 저장 (orderId={}, eventStatus={})",
                order.getId(), orderCreatedOutbox.getStatus());

        return OrderQueryResponse.from(order);
    }

    /**
     * Mock PG 실패. PAYMENT_PENDING → PAYMENT_FAILED, Outbox 저장 없음.
     */
    @Transactional
    public OrderQueryResponse failMockPayment(User user, Long orderId, MockPaymentFailRequest request) {
        getOrderer(user);

        Order order = orderRepository.findByIdAndUserIdWithItems(orderId, user.getId())
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == Status.PAYMENT_FAILED) {
            return OrderQueryResponse.from(order);
        }
        if (order.getStatus() != Status.PAYMENT_PENDING) {
            throw new CustomException(OrderErrorCode.INVALID_PAYMENT_STATE);
        }

        String reason = request.getReason() != null && !request.getReason().isBlank()
                ? request.getReason().trim()
                : "mock payment failed";

        Status from = order.getStatus();
        order.markPaymentFailed();
        recordStatusHistory(order, from, Status.PAYMENT_FAILED, reason);
        orderRepository.save(order);
        log.info("[MOCK-PAYMENT] - 결제 실패 처리 (orderId={})", orderId);

        return OrderQueryResponse.from(order);
    }

    @Transactional
    public OrderQueryResponse cancel(User user, Long orderId) {
        getOrderer(user);

        Order order = orderRepository.findByIdAndUserIdWithItems(orderId, user.getId())
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));

        if (!order.cancellable()) {
            throw new CustomException(OrderErrorCode.ORDER_NOT_CANCELLABLE);
        }

        for (OrderItem item : order.getOrderItems()) {
            Merchandise merchandise = merchandiseRepository.findByIdAndDeletedAtIsNullForUpdate(item.getProductId())
                    .orElseThrow(() -> new CustomException(MerchandiseErrorCode.MERCHANDISE_NOT_FOUND));
            merchandise.increaseStock(item.getQuantity());
        }

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

    private User getOrderer(User user) {
        return userRepository.findByIdAndDeletedAtIsNull(user.getId())
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }

    private BigDecimal resolveUnitPrice(Merchandise merchandise) {
        Long salePrice = merchandise.getSalePrice();
        Long price = salePrice != null ? salePrice : merchandise.getPrice();
        return BigDecimal.valueOf(price);
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
