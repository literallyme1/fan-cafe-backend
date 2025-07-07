package com.example.fan_cafe.schedule.interfaces.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Builder
@Getter
public class ScheduleListResponse {
    private LocalDate date;
    private List<ScheduleResponse> schedules;

    public static ScheduleListResponse of(LocalDate date, List<ScheduleResponse> scheduleList) {
        return ScheduleListResponse.builder()
                .date(date)
                .schedules(scheduleList)
                .build();
    }
}
