package com.example.fan_cafe.order.interfaces.dto;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
public class OrderCreateRequest {

    // 주문 생성 시 전달되는 상품 목록.
    @NotEmpty(message = "주문 상품은 최소 1개 이상이어야 합니다.")
    @Valid
    @Schema(description = "주문 상품 목록", example = "[{\"productId\":501,\"quantity\":2}]")
    private List<Item> items;

    @Getter
    public static class Item {
        // 주문 대상 상품 ID.
        @NotNull(message = "productId는 필수입니다.")
        @Schema(description = "상품 식별자", example = "501")
        private Long productId;

        // 주문 수량(최소 1개).
        @NotNull(message = "quantity는 필수입니다.")
        @Min(value = 1, message = "주문 수량은 1 이상이어야 합니다.")
        @Schema(description = "주문 수량", example = "2")
        private Integer quantity;
    }
}
