package com.example.fan_cafe.outbox.infrastructure;

import com.example.fan_cafe.outbox.domain.DlqEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DlqEventRepository extends JpaRepository<DlqEvent, Long> {

    List<DlqEvent> findAllByOrderByCreatedAtDesc();

    Optional<DlqEvent> findTopByEventIdOrderByCreatedAtDesc(String eventId);
}
