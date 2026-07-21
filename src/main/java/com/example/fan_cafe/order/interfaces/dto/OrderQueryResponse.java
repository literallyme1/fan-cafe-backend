package com.example.fan_cafe.order.interfaces.dto;

import com.example.fan_cafe.order.domain.Order;
import com.example.fan_cafe.order.domain.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderQueryResponse {

    @Schema(description = "주문 식별자", example = "10001")
    private Long orderId;
    @Schema(description = "주문 상태", example = "PAID")
    private Status status;
    @Schema(description = "총 주문 금액", example = "59000")
    private BigDecimal totalPrice;
    @Schema(description = "주문 시각", example = "2026-07-21T18:30:00")
    private LocalDateTime createdAt;
    @Schema(description = "주문 상품 목록", example = "[{\"productId\":501,\"productName\":\"2026 월드투어 공식 티셔츠\",\"price\":29500,\"quantity\":2}]")
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
