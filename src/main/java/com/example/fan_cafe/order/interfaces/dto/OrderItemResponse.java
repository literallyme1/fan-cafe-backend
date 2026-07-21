package com.example.fan_cafe.order.interfaces.dto;

import com.example.fan_cafe.order.domain.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class OrderItemResponse {

    @Schema(description = "상품 식별자", example = "501")
    private Long productId;
    @Schema(description = "상품명", example = "2026 월드투어 공식 티셔츠")
    private String productName;
    @Schema(description = "주문 단가", example = "29500")
    private BigDecimal price;
    @Schema(description = "주문 수량", example = "2")
    private Integer quantity;

    // 주문 엔티티의 항목 스냅샷을 응답 DTO로 변환한다.
    public static OrderItemResponse from(OrderItem item) {
        return OrderItemResponse.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .build();
    }
}
