package com.example.fan_cafe.order.infrastructure;

import com.example.fan_cafe.order.domain.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    long countByOrder_Id(Long orderId);

    void deleteByOrder_Id(Long orderId);
}
