package com.example.fan_cafe.schedule.infrastructure;

import com.example.fan_cafe.schedule.domain.Schedule;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @NonNull
    Optional<Schedule> findById(Long id);

    List<Schedule> findByStartAtBetween(LocalDateTime start, LocalDateTime end);
}
