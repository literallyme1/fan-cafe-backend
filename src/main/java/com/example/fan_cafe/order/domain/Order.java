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

    // 주문 생성 시 Mock PG 승인 전까지 PAYMENT_PENDING으로 둔다.
    public static Order paymentPending(User user, BigDecimal totalPrice) {
        return Order.builder()
                .user(user)
                .totalPrice(totalPrice)
                .status(Status.PAYMENT_PENDING)
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

    // Mock PG 승인 성공 시 PAID로 전이한다.
    public void markPaid() {
        this.status = Status.PAID;
    }

    // 승인 금액 불일치·Mock PG 실패 API 호출 시 PAYMENT_FAILED로 전이한다.
    public void markPaymentFailed() {
        this.status = Status.PAYMENT_FAILED;
    }

    // 현재 주문 상태가 취소 가능한지 도메인 규칙으로 판단한다.
    /** 결제 전(PAYMENT_PENDING)에만 주문 취소 가능. 결제 후 환불은 별도 Mock 환불 API 사용. */
    public boolean cancellable() {
        return this.status == Status.PAYMENT_PENDING;
    }

    // 취소 가능 상태일 때만 취소 상태로 변경한다.
    public void cancel() {
        this.status = Status.CANCELLED;
    }

    /** Mock PG 전체 환불 — PAID → REFUNDED */
    public void markRefunded() {
        this.status = Status.REFUNDED;
    }

    // 외부에서 리스트를 직접 수정하지 못하도록 읽기 전용 뷰만 반환한다.
    public List<OrderItem> getOrderItems() {
        return Collections.unmodifiableList(orderItems);
    }
}
