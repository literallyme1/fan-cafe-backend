package com.example.fan_cafe.schedule.interfaces.dto;


import com.example.fan_cafe.schedule.domain.Schedule;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Getter
@NoArgsConstructor
public class ScheduleResponse {

    private Long id;
    private String title;
    private String location;
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    @QueryProjection
    public ScheduleResponse(Long id, String title, String location, LocalDateTime startAt, LocalDateTime endAt) {
        this.id = id;
        this.title = title;
        this.location = location;
        this.startAt = startAt;
        this.endAt = endAt;
    }

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
