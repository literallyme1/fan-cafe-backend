package com.example.fan_cafe.order.interfaces.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
public class OrderCreateRequest {

    @NotEmpty(message = "주문 상품은 최소 1개 이상이어야 합니다.")
    @Valid
    private List<Item> items;

    @Getter
    public static class Item {
        @NotNull(message = "productId는 필수입니다.")
        private Long productId;

        @NotNull(message = "quantity는 필수입니다.")
        @Min(value = 1, message = "주문 수량은 1 이상이어야 합니다.")
        private Integer quantity;
    }
}

