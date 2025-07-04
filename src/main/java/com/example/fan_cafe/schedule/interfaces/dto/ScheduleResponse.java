package com.example.fan_cafe.schedule.interfaces.dto;


import com.example.fan_cafe.schedule.domain.Schedule;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class ScheduleResponse {

    private Long id;
    private String title;
    private String location;
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    public static ScheduleResponse from(Schedule schedule) {
        return ScheduleResponse.builder()
                .id(schedule.getId())
                .title(schedule.getTitle())
                .location(schedule.getLocation())
                .startAt(schedule.getStartAt())
                .endAt(schedule.getEndAt())
                .build();
    }

}
