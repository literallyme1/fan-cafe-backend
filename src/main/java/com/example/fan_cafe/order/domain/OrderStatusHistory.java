package com.example.fan_cafe.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 주문 상태 전이 이력. 결제 Mock 흐름(PAYMENT_PENDING → PAID / PAYMENT_FAILED) 추적용.
 */
@Entity
@Table(name = "order_status_history")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false)
    private Status fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private Status toStatus;

    @Column(length = 500)
    private String reason;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    public static OrderStatusHistory of(Order order, Status fromStatus, Status toStatus, String reason) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.order = order;
        history.fromStatus = fromStatus;
        history.toStatus = toStatus;
        history.reason = reason;
        return history;
    }
}
