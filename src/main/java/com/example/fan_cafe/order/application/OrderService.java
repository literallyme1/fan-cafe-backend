package com.example.fan_cafe.order.application;

import com.example.fan_cafe.global.exception.CustomException;
import com.example.fan_cafe.global.exception.GlobalErrorCode;
import com.example.fan_cafe.merchandise.domain.Merchandise;
import com.example.fan_cafe.merchandise.exception.MerchandiseErrorCode;
import com.example.fan_cafe.merchandise.infrastructure.MerchandiseRepository;
import com.example.fan_cafe.order.domain.Order;
import com.example.fan_cafe.order.domain.OrderItem;
import com.example.fan_cafe.order.exception.OrderErrorCode;
import com.example.fan_cafe.order.infrastructure.OrderRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserRepository userRepository;
    private final MerchandiseRepository merchandiseRepository;
    private final OrderRepository orderRepository;
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

    // 로그인 사용자의 주문 목록을 최신순으로 조회한다.
    @Transactional(readOnly = true)
    public List<OrderQueryResponse> getMyOrders(User user) {
        getOrderer(user);
        return orderRepository.findAllByUserIdWithItems(user.getId()).stream()
                .map(OrderQueryResponse::from)
                .toList();
    }

    @Transactional
    public OrderCreateResponse create(User user, OrderCreateRequest request) {
        // 인증 사용자 기준으로 실제 주문자 엔티티를 다시 조회해 유효성을 보장한다.
        User orderer = getOrderer(user);

        // 빈 주문 요청은 비즈니스 규칙 위반으로 처리한다.
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new CustomException(OrderErrorCode.ORDER_ITEMS_REQUIRED);
        }

        // 주문 생성 시점에는 항목이 아직 없으므로 총액 0으로 시작한다.
        Order order = Order.paid(orderer, BigDecimal.ZERO);
        BigDecimal total = BigDecimal.ZERO;

        for (OrderCreateRequest.Item item : request.getItems()) {
            // null quantity를 방지하고 도메인 검증을 단일 지점에서 수행한다.
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            if (quantity < 1) {
                throw new CustomException(OrderErrorCode.INVALID_QUANTITY);
            }

            // 재고 차감을 위해 비관적 락으로 상품 행을 잠근다.
            Merchandise merchandise = merchandiseRepository.findByIdAndDeletedAtIsNullForUpdate(item.getProductId())
                    .orElseThrow(() -> new CustomException(MerchandiseErrorCode.MERCHANDISE_NOT_FOUND));

            BigDecimal unitPrice = resolveUnitPrice(merchandise);
            merchandise.decreaseStock(quantity);

            // 가격/이름 기준의 스냅샷을 저장해 이후 상품 정보 변경과 주문 내역을 분리한다.
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

        // 주문 생성 이벤트를 outbox에 저장해 비동기 발행을 보장한다.
        outboxEventRepository.save(OutboxEvent.init("ORDER", saved.getId(), buildOrderCreatedPayload(saved)));
        return OrderCreateResponse.from(saved);
    }

    @Transactional
    public OrderQueryResponse cancel(User user, Long orderId) {
        getOrderer(user);

        // 1) 주문 조회(본인 소유 + 주문항목 fetch join)
        Order order = orderRepository.findByIdAndUserIdWithItems(orderId, user.getId())
                .orElseThrow(() -> new CustomException(OrderErrorCode.ORDER_NOT_FOUND));

        // 2) 상태 확인(취소 가능 여부)
        if (!order.cancellable()) {
            throw new CustomException(OrderErrorCode.ORDER_NOT_CANCELLABLE);
        }

        // 3) 재고 복구(상품별 비관적 락 조회 후 수량 복구)
        for (OrderItem item : order.getOrderItems()) {
            Merchandise merchandise = merchandiseRepository.findByIdAndDeletedAtIsNullForUpdate(item.getProductId())
                    .orElseThrow(() -> new CustomException(MerchandiseErrorCode.MERCHANDISE_NOT_FOUND));
            merchandise.increaseStock(item.getQuantity());
        }

        // 4) 상태 변경(CANCELLED)
        order.cancel();

        // 5) outbox 이벤트 생성
        outboxEventRepository.save(OutboxEvent.init("ORDER", order.getId(), buildOrderCancelledPayload(order)));

        return OrderQueryResponse.from(order);
    }

    private User getOrderer(User user) {
        return userRepository.findByIdAndDeletedAtIsNull(user.getId())
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }

    private BigDecimal resolveUnitPrice(Merchandise merchandise) {
        // 세일가가 있으면 우선 적용하고, 없으면 일반 판매가를 사용한다.
        Long salePrice = merchandise.getSalePrice();
        Long price = salePrice != null ? salePrice : merchandise.getPrice();
        return BigDecimal.valueOf(price);
    }

    private String buildOrderCreatedPayload(Order order) {
        // 소비자(consumer)에서 바로 사용할 수 있도록 이벤트 payload를 평탄화한다.
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

