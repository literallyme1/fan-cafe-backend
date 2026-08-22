package com.example.fan_cafe.order.infrastructure;

import com.example.fan_cafe.order.domain.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("""
            select distinct o
            from Order o
            left join fetch o.orderItems oi
            where o.id = :orderId
              and o.user.id = :userId
              and o.deletedAt is null
            """)
    Optional<Order> findByIdAndUserIdWithItems(@Param("orderId") Long orderId, @Param("userId") Long userId);

    @Query("""
            select distinct o
            from Order o
            left join fetch o.orderItems oi
            where o.user.id = :userId
              and o.deletedAt is null
            order by o.createdAt desc
            """)
    List<Order> findAllByUserIdWithItems(@Param("userId") Long userId);

    @Query("""
            select distinct o
            from Order o
            left join fetch o.orderItems oi
            where o.id = :orderId
              and o.deletedAt is null
            """)
    Optional<Order> findByIdWithItems(@Param("orderId") Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select o
            from Order o
            where o.id = :orderId
              and o.deletedAt is null
            """)
    Optional<Order> findPaymentOrderWithPessimisticLock(@Param("orderId") Long orderId);
}
