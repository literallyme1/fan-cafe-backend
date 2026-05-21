package com.example.fan_cafe.order.infrastructure;

import com.example.fan_cafe.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // 내 주문 단건 조회 시 주문 항목까지 한 번에 조회해 N+1을 방지한다.
    @Query("""
            select distinct o
            from Order o
            left join fetch o.orderItems oi
            where o.id = :orderId
              and o.user.id = :userId
              and o.deletedAt is null
            """)
    Optional<Order> findByIdAndUserIdWithItems(@Param("orderId") Long orderId, @Param("userId") Long userId);

    // 내 주문 목록 조회 시 주문 항목을 fetch join으로 함께 로딩한다.
    @Query("""
            select distinct o
            from Order o
            left join fetch o.orderItems oi
            where o.user.id = :userId
              and o.deletedAt is null
            order by o.createdAt desc
            """)
    List<Order> findAllByUserIdWithItems(@Param("userId") Long userId);

    // Mock PG 웹훅: 주문자 검증 없이 주문+항목 조회
    @Query("""
            select distinct o
            from Order o
            left join fetch o.orderItems oi
            where o.id = :orderId
              and o.deletedAt is null
            """)
    Optional<Order> findByIdWithItems(@Param("orderId") Long orderId);
}

