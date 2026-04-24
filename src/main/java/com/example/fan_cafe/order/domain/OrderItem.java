package com.example.fan_cafe.order.domain;

import com.example.fan_cafe.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
@Table(name = "order_items")
public class OrderItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;

    // 주문 시점의 상품 정보 스냅샷을 만들어 주문 이력을 보존한다.
    public static OrderItem snapshot(Long productId, String productName, BigDecimal price, int quantity) {
        return OrderItem.builder()
                .productId(productId)
                .productName(productName)
                .price(price)
                .quantity(quantity)
                .build();
    }

    // Order.addItem() 내부에서만 호출되어 연관관계를 일관되게 유지한다.
    void attachTo(Order order) {
        this.order = order;
    }
}

