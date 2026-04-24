package com.example.fan_cafe.order.interfaces.dto;

import com.example.fan_cafe.order.domain.Order;
import com.example.fan_cafe.order.domain.Status;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderQueryResponse {

    private Long orderId;
    private Status status;
    private BigDecimal totalPrice;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;

    // 주문과 주문 항목을 조회 응답 DTO로 변환한다.
    public static OrderQueryResponse from(Order order) {
        return OrderQueryResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .createdAt(order.getCreatedAt())
                .items(order.getOrderItems().stream()
                        .map(OrderItemResponse::from)
                        .toList())
                .build();
    }
}
