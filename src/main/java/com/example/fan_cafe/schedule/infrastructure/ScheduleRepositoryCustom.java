package com.example.fan_cafe.schedule.infrastructure;

import com.example.fan_cafe.schedule.interfaces.dto.ScheduleResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleRepositoryCustom {

    List<ScheduleResponse> findByStartAtBetween(LocalDateTime start, LocalDateTime end);
}
