package com.example.fan_cafe.outbox.infrastructure;

import com.example.fan_cafe.outbox.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    // outbox 이벤트 저장/조회는 기본 JPA 메서드로 처리한다.
}

