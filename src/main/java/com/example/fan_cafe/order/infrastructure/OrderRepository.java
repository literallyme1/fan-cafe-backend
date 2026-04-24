package com.example.fan_cafe.order.infrastructure;

import com.example.fan_cafe.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // 기본 CRUD는 JpaRepository의 표준 메서드를 사용한다.
}

