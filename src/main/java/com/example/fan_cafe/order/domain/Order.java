package com.example.fan_cafe.order.domain;

import com.example.fan_cafe.global.common.BaseTimeEntity;
import com.example.fan_cafe.user.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder.Default;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.math.BigDecimal;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
@Table(name = "orders")
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Default
    private List<OrderItem> orderItems = new ArrayList<>();

    // 즉시 결제 완료 플로우에서 생성되는 주문이라 초기 상태를 PAID로 둔다.
    public static Order paid(User user, BigDecimal totalPrice) {
        return Order.builder()
                .user(user)
                .totalPrice(totalPrice)
                .status(Status.PAID)
                .build();
    }

    // 주문 항목이 변경된 뒤 서비스 계층에서 총액을 다시 계산해 반영할 때 사용한다.
    public void updateTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    // 연관관계의 주인을 OrderItem 쪽으로 맞춰 양방향 참조를 함께 세팅한다.
    public void addItem(OrderItem item) {
        this.orderItems.add(item);
        item.attachTo(this);
    }

    // 외부에서 리스트를 직접 수정하지 못하도록 읽기 전용 뷰만 반환한다.
    public List<OrderItem> getOrderItems() {
        return Collections.unmodifiableList(orderItems);
    }
}
