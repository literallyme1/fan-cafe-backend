package com.example.fan_cafe.order.interfaces.dto;

import com.example.fan_cafe.order.domain.Order;
import com.example.fan_cafe.order.domain.Status;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Builder
@Getter
public class OrderCreateResponse {

    // 생성된 주문 식별자.
    private Long orderId;
    // 주문 생성 직후 상태.
    private Status status;
    // 계산 완료된 주문 총액.
    private BigDecimal totalPrice;

    // 도메인 엔티티를 API 응답 전용 DTO로 변환한다.
    public static OrderCreateResponse from(Order order) {
        return OrderCreateResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .build();
    }
}

