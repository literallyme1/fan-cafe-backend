package com.example.fan_cafe.schedule.interfaces.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MonthlyScheduleResponse {


    private List<ScheduleListResponse> schedules;

    public static MonthlyScheduleResponse of(List<ScheduleListResponse> schedules) {
        return MonthlyScheduleResponse.builder()
                .schedules(schedules)
                .build();
    }
}
