package com.example.fan_cafe.schedule.infrastructure;

import com.example.fan_cafe.schedule.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByStartAtBetween(LocalDateTime start, LocalDateTime end);
}
