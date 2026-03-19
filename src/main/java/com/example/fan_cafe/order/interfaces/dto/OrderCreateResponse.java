package com.example.fan_cafe.order.interfaces.dto;

import com.example.fan_cafe.order.domain.Order;
import com.example.fan_cafe.order.domain.Status;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Builder
@Getter
public class OrderCreateResponse {

    private Long orderId;
    private Status status;
    private BigDecimal totalPrice;

    public static OrderCreateResponse from(Order order) {
        return OrderCreateResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .build();
    }
}

